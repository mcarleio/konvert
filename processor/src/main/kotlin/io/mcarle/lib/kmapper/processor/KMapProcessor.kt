package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.lib.kmapper.converter.api.ConverterConfig
import io.mcarle.lib.kmapper.converter.api.Options
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.processor.kmapfrom.KMapFromCodeGenerator
import io.mcarle.lib.kmapper.processor.kmapfrom.KMapFromConverter
import io.mcarle.lib.kmapper.processor.kmapfrom.KMapFromConverterCollector
import io.mcarle.lib.kmapper.processor.kmapper.KMapperCodeGenerator
import io.mcarle.lib.kmapper.processor.kmapper.KMapperConverter
import io.mcarle.lib.kmapper.processor.kmapper.KMapperConverterCollector
import io.mcarle.lib.kmapper.processor.kmapto.KMapToCodeGenerator
import io.mcarle.lib.kmapper.processor.kmapto.KMapToConverter
import io.mcarle.lib.kmapper.processor.kmapto.KMapToConverterCollector
import io.mcarle.lib.kmapper.processor.codegen.CodeBuilder

class KMapProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Options,
    private val logger: KSPLogger,
) : SymbolProcessor {


    @Synchronized
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val typeConverters = collectTypeConverters(resolver)
        registerTypeConverters(typeConverters)

        initCodeBuilder()
        initTypeConverters(resolver)

        generateMappingCode(resolver, typeConverters)

        writeFiles()

        return emptyList()
    }

    private fun writeFiles() {
        CodeBuilder.all().forEach {
            it.build().writeTo(
                codeGenerator,
                aggregating = true, // always aggregating, as any new file could be a mapper with higher prio than a potentially used one.
                it.originating
            )
        }
    }

    private fun generateMappingCode(resolver: Resolver, typeConverters: List<AnnotatedConverter>) {
        typeConverters.forEach {
            when (it) {
                is KMapToConverter -> KMapToCodeGenerator.generate(it, resolver, logger)
                is KMapFromConverter -> KMapFromCodeGenerator.generate(it, resolver, logger)
                is KMapperConverter -> KMapperCodeGenerator.generate(it, resolver, logger)
                else -> throw RuntimeException("Unknown converter: ${it::class}")
            }
        }
    }

    private fun registerTypeConverters(typeConverters: List<AnnotatedConverter>) {
        typeConverters
            .groupBy { it.priority }
            .forEach { (priority, list) ->
                TypeConverterRegistry.addConverters(priority, list)
            }
    }

    private fun initTypeConverters(resolver: Resolver) {
        TypeConverterRegistry.initConverters(ConverterConfig(resolver, options))
    }

    private fun initCodeBuilder() {
        CodeBuilder.clear()
    }

    private fun collectTypeConverters(resolver: Resolver): List<AnnotatedConverter> {
        return KMapToConverterCollector.collect(resolver) +
                KMapFromConverterCollector.collect(resolver) +
                KMapperConverterCollector.collect(resolver, logger)
    }
}