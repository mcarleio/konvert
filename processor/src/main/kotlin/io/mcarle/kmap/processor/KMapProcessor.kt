package io.mcarle.kmap.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.kmap.converter.api.ConverterConfig
import io.mcarle.kmap.converter.api.Options
import io.mcarle.kmap.converter.api.TypeConverterRegistry
import io.mcarle.kmap.processor.codegen.CodeBuilder
import io.mcarle.kmap.processor.exceptions.UnexpectedTypeConverter
import io.mcarle.kmap.processor.kmapfrom.KMapFromCodeGenerator
import io.mcarle.kmap.processor.kmapfrom.KMapFromConverter
import io.mcarle.kmap.processor.kmapfrom.KMapFromConverterCollector
import io.mcarle.kmap.processor.kmapper.KMapperCodeGenerator
import io.mcarle.kmap.processor.kmapper.KMapperConverter
import io.mcarle.kmap.processor.kmapper.KMapperConverterCollector
import io.mcarle.kmap.processor.kmapto.KMapToCodeGenerator
import io.mcarle.kmap.processor.kmapto.KMapToConverter
import io.mcarle.kmap.processor.kmapto.KMapToConverterCollector

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
                else -> throw UnexpectedTypeConverter(it)
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
