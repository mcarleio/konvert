package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.api.annotation.KMapFrom
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.api.annotation.KMapper
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.converter.api.ConverterConfig
import io.mcarle.lib.kmapper.converter.api.Options
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.processor.converter.annotated.*
import java.util.*

class KMapProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
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
        KMapFromCodeGenerator.write(codeGenerator)
        KMapperCodeGenerator.write(codeGenerator)
    }

    private fun generateMappingCode(resolver: Resolver, typeConverters: List<AnnotatedConverter>) {
        KMapToCodeGenerator.init()
        KMapFromCodeGenerator.init()
        KMapperCodeGenerator.init()
        typeConverters.forEach {
            when (it) {
                is KMapToConverter -> KMapToCodeGenerator.generate(it, resolver, logger)
                is KMapFromConverter -> KMapFromCodeGenerator.generate(it, resolver, logger)
                is KMapperConverter -> KMapperCodeGenerator.generate(it, resolver, logger)
            }
        }
    }

    private fun initConverters(resolver: Resolver): List<AnnotatedConverter> {
        val typeConverters = collectTypeConverters(resolver)
        typeConverters
            .groupBy { it.priority }
            .forEach { (priority, list) ->
                TypeConverterRegistry.addConverters(priority, list)
            }
        TypeConverterRegistry.initConverters(ConverterConfig(resolver, options))
        return typeConverters
    }

    private fun collectTypeConverters(resolver: Resolver): List<AnnotatedConverter> {
        return collectTypeConvertersForKMapTo(resolver) +
                collectTypeConvertersForKMapFrom(resolver) +
                collectTypeConvertersForKMapping(resolver)
    }

    @OptIn(KspExperimental::class)
    private fun collectTypeConvertersForKMapping(resolver: Resolver): List<KMapperConverter> {
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

    private fun collectTypeConvertersForKMapTo(resolver: Resolver): List<KMapToConverter> {
        return resolver.getSymbolsWithAnnotation(KMapTo::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.CLASS) {
                    throw IllegalStateException("KMap can only target classes and companion objects")
                }


                ksClassDeclaration.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapTo::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KMapTo.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KMapToConverter.AnnotationData.from(it)
                    }
                    .map {
                        KMapToConverter(
                            annotationData = it,
                            sourceClassDeclaration = ksClassDeclaration,
                            targetClassDeclaration = it.value
                        )
                    }
            }.toList()
    }

    private fun collectTypeConvertersForKMapFrom(resolver: Resolver): List<KMapFromConverter> {
        return resolver.getSymbolsWithAnnotation(KMapFrom::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val annotatedDeclaration = ksAnnotated as? KSClassDeclaration
                    ?: throw IllegalStateException("KMapFrom can only target class declarations or companion objects")

                val (targetKsClassDeclaration, targetCompanionDeclaration) = determineClassAndCompanion(
                    annotatedDeclaration = annotatedDeclaration
                )

                ksAnnotated.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapFrom::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KMapFrom.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KMapFromConverter.AnnotationData.from(it)
                    }
                    .map {
                        KMapFromConverter(
                            annotationData = it,
                            sourceClassDeclaration = it.value,
                            targetClassDeclaration = targetKsClassDeclaration,
                            targetCompanionDeclaration = targetCompanionDeclaration
                        )
                    }
            }.toList()
    }

    private fun determineClassAndCompanion(annotatedDeclaration: KSClassDeclaration): Pair<KSClassDeclaration, KSClassDeclaration> {
        return if (annotatedDeclaration.isCompanionObject) {
            val targetKsClassDeclaration = annotatedDeclaration.parentDeclaration as? KSClassDeclaration
                ?: throw RuntimeException("Parent of $annotatedDeclaration is no class declaration")
            if (targetKsClassDeclaration.classKind != ClassKind.CLASS) {
                throw RuntimeException("Parent of $annotatedDeclaration is not ${ClassKind.CLASS} but is ${targetKsClassDeclaration.classKind}")
            }
            val targetCompanionDeclaration = annotatedDeclaration
            targetKsClassDeclaration to targetCompanionDeclaration
        } else if (annotatedDeclaration.classKind == ClassKind.CLASS) {
            val targetCompanionDeclaration =
                annotatedDeclaration.declarations
                    .firstOrNull { (it as? KSClassDeclaration)?.isCompanionObject ?: false } as? KSClassDeclaration
                    ?: throw RuntimeException("Missing Companion for $annotatedDeclaration")
            val targetKsClassDeclaration = annotatedDeclaration
            targetKsClassDeclaration to targetCompanionDeclaration
        } else {
            throw RuntimeException("KMapFrom only allowed on compantion objects or class declarations with a companion")
        }
    }

}