package io.mcarle.lib.kmapper.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping

@AutoService(SymbolProcessorProvider::class)
class KMapperProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KMapperProcessor(environment.codeGenerator, environment.logger)
    }
}

class KMapperProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {


    @Synchronized
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val mapper = Mapper(
            resolver = resolver,
            logger = logger
        )

        val kspMappersConverters = collectKMappersConverter(resolver)
        kspMappersConverters.forEach { TypeConverterRegistry += it.second }

        TypeConverterRegistry.initConverter(
            ConverterConfig(
                resolver = resolver,
                enforceNotNull = true
            )
        )

        val builderCache = mutableMapOf<Pair<String, String>, Pair<Pair<FileSpec.Builder, TypeSpec.Builder>, List<KSFile>>>()
        kspMappersConverters.forEach { (mappingAnnotation, converter) ->
            val typeSpecBuilder = findOrBuildObjectBuilder(
                builderCache,
                converter.mapKSClassDeclaration.packageName.asString(),
                converter.mapKSClassDeclaration.asStarProjectedType(),
                converter.mapKSClassDeclaration.simpleName.asString(),
                converter.mapKSClassDeclaration.containingFile
            )

            typeSpecBuilder.addFunction(
                FunSpec.builder(converter.mapFunctionName)
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(converter.targetClassDeclaration.asType(emptyList()).toTypeName())
                    .addParameter(converter.paramName, converter.sourceClassDeclaration.asType(emptyList()).toTypeName())
                    .addCode(
                        mapper.rules(
                            mappingAnnotation,
                            converter.paramName,
                            converter.sourceClassDeclaration,
                            converter.targetClassDeclaration
                        )
                    )
                    .build()
            )
        }

        builderCache.values.forEach { (builderPair, originatingKSFiles) ->
            val (fileSpecBuilder, typeSpecBuilder) = builderPair
            fileSpecBuilder.addType(typeSpecBuilder.build())
            fileSpecBuilder.build().writeTo(
                codeGenerator,
                aggregating = false,
                originatingKSFiles = originatingKSFiles
            )
        }

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun collectKMappersConverter(resolver: Resolver): Sequence<Pair<KMapping, KMapperConverter>> {
        return resolver.getSymbolsWithAnnotation(KMapper::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.INTERFACE) {
                    throw IllegalStateException("KMap can only target interfaces")
                }

                ksClassDeclaration
                    .getAllFunctions()
                    .filter { it.isAnnotationPresent(KMapping::class) }
                    .map {
                        val source =
                            if (it.parameters.size > 1) null else it.parameters.first().type.resolve().declaration as? KSClassDeclaration
                        val target = it.returnType?.resolve()?.declaration as? KSClassDeclaration

                        val kspMapping = it.getAnnotationsByType(KMapping::class).first()

                        if (source == null || target == null) {
                            throw IllegalStateException("KMapping annotated function must have exactly one parameter: $it")
                        }

                        kspMapping to KMapperConverter(
                            sourceClassDeclaration = source,
                            targetClassDeclaration = target,
                            mapKSClassDeclaration = ksClassDeclaration,
                            mapFunctionName = it.simpleName.asString(),
                            paramName = it.parameters.first().name!!.asString()
                        )

                    }
            }
    }

    private fun findOrBuildObjectBuilder(
        cache: MutableMap<Pair<String, String>, Pair<Pair<FileSpec.Builder, TypeSpec.Builder>, List<KSFile>>>,
        packageName: String,
        interfaceType: KSType,
        interfaceName: String,
        file: KSFile?
    ): TypeSpec.Builder {
        val identifier = packageName to "${interfaceName}Impl"
        val pair = cache[identifier]
        if (pair == null) {
            cache[identifier] = (FileSpec.builder(
                packageName = packageName,
                fileName = "${interfaceName}Impl"
            ) to TypeSpec.objectBuilder("${interfaceName}Impl").addSuperinterface(interfaceType.toTypeName())) to listOfNotNull(file)
        } else {
            cache[identifier] = pair.first to (pair.second + file).filterNotNull().distinct()
        }
        return cache[identifier]!!.first.second
    }

}
