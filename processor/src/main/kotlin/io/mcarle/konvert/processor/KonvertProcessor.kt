package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.exceptions.UnexpectedTypeConverter
import io.mcarle.konvert.processor.konvert.KonvertCodeGenerator
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvert.KonvertTypeConverterCollector
import io.mcarle.konvert.processor.konvertfrom.KonvertFromCodeGenerator
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverterCollector
import io.mcarle.konvert.processor.konvertto.KonvertToCodeGenerator
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverterCollector

class KonvertProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: io.mcarle.konvert.converter.api.Options,
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

    private fun generateMappingCode(resolver: Resolver, typeConverters: List<io.mcarle.konvert.processor.AnnotatedConverter>) {
        typeConverters.forEach {
            when (it) {
                is KonvertToTypeConverter -> KonvertToCodeGenerator.generate(it, resolver, logger)
                is KonvertFromTypeConverter -> KonvertFromCodeGenerator.generate(it, resolver, logger)
                is KonvertTypeConverter -> KonvertCodeGenerator.generate(it, resolver, logger)
                else -> throw UnexpectedTypeConverter(it)
            }
        }
    }

    private fun registerTypeConverters(typeConverters: List<io.mcarle.konvert.processor.AnnotatedConverter>) {
        typeConverters
            .groupBy { it.priority }
            .forEach { (priority, list) ->
                TypeConverterRegistry.addConverters(priority, list)
            }
    }

    private fun initTypeConverters(resolver: Resolver) {
        TypeConverterRegistry.initConverters(io.mcarle.konvert.converter.api.ConverterConfig(resolver, options))
    }

    private fun initCodeBuilder() {
        CodeBuilder.clear()
    }

    private fun collectTypeConverters(resolver: Resolver): List<io.mcarle.konvert.processor.AnnotatedConverter> {
        return KonvertToTypeConverterCollector.collect(resolver) +
            KonvertFromTypeConverterCollector.collect(resolver) +
            KonvertTypeConverterCollector.collect(resolver, logger)
    }
}
