package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.annotation.KMapTo
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping
import io.mcarle.lib.kmapper.processor.config.CliOptions
import io.mcarle.lib.kmapper.processor.converter.annotated.*

class KMapProcessor(
    private val codeGenerator: CodeGenerator,
    private val cliOptions: CliOptions,
    private val logger: KSPLogger,
) : SymbolProcessor {


    @Synchronized
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val typeConverters = initConverters(resolver)

        generateMappingCode(resolver, typeConverters)

        writeFiles()

        return emptyList()
    }

    private fun writeFiles() {
        KMapToCodeGenerator.write(codeGenerator)
        KMapperCodeGenerator.write(codeGenerator)
    }

    private fun generateMappingCode(resolver: Resolver, typeConverters: List<AnnotatedConverter<*>>) {
        KMapToCodeGenerator.init()
        KMapperCodeGenerator.init()
        typeConverters.forEach {
            when (it) {
                is KMapToConverter -> KMapToCodeGenerator.generate(it, resolver, logger)
                is KMapperConverter -> KMapperCodeGenerator.generate(it, resolver, logger)
            }
        }
    }

    private fun initConverters(resolver: Resolver): List<AnnotatedConverter<*>> {
        val typeConverters = collectTypeConverters(resolver)
        typeConverters
            .groupBy { it.priority }
            .forEach { (priority, list) ->
                TypeConverterRegistry.addConverters(priority, list)
            }
        TypeConverterRegistry.initConverters(cliOptions.toConverterConfig(resolver))
        return typeConverters
    }

    fun collectTypeConverters(resolver: Resolver): List<AnnotatedConverter<*>> {
        return collectTypeConvertersForKMapTo(resolver) +
                collectTypeConvertersForKMapping(resolver)
    }

    @OptIn(KspExperimental::class)
    fun collectTypeConvertersForKMapping(resolver: Resolver): List<KMapperConverter> {
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

                        KMapperConverter(
                            annotation = kspMapping,
                            sourceClassDeclaration = source,
                            targetClassDeclaration = target,
                            mapKSClassDeclaration = ksClassDeclaration,
                            mapKSFunctionDeclaration = it
                        )
                    }
            }.toList()
    }

    @OptIn(KspExperimental::class)
    fun collectTypeConvertersForKMapTo(resolver: Resolver): List<KMapToConverter> {
        return resolver.getSymbolsWithAnnotation(KMapTo::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                var companion = false
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration != null && ksClassDeclaration.isCompanionObject) {
                    companion = true
                } else if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.CLASS) {
                    throw IllegalStateException("KMap can only target classes and companion objects")
                }


                val kspMapToAnnotation = ksClassDeclaration.annotations.first {
                    (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapTo::class.qualifiedName
                }

                val kspMapping = ksClassDeclaration.getAnnotationsByType(KMapTo::class).first()

                val targetKsClassDeclarations = listOfNotNull((kspMapToAnnotation.arguments.first {
                    it.name?.asString() == KMapTo::value.name
                }.value as? KSType)?.declaration as? KSClassDeclaration)

                targetKsClassDeclarations.map { targetKsClassDeclaration ->
                    KMapToConverter(
                        annotation = kspMapping,
                        sourceClassDeclaration = ksClassDeclaration,
                        targetClassDeclaration = targetKsClassDeclaration,
                        mapKSClassDeclaration = ksClassDeclaration,
                        mapFunctionName = kspMapping.mapFunctionName.ifEmpty { "mapTo${targetKsClassDeclaration.toClassName().simpleName}" }
                    )
                }

            }.toList()
    }

}