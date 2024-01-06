package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.konvert.KonverterCodeGenerator
import io.mcarle.konvert.processor.konvert.KonverterData
import io.mcarle.konvert.processor.konvert.KonverterDataCollector
import io.mcarle.konvert.processor.konvertfrom.KonvertFromCodeGenerator
import io.mcarle.konvert.processor.konvertfrom.KonvertFromData
import io.mcarle.konvert.processor.konvertfrom.KonvertFromDataCollector
import io.mcarle.konvert.processor.konvertto.KonvertToCodeGenerator
import io.mcarle.konvert.processor.konvertto.KonvertToData
import io.mcarle.konvert.processor.konvertto.KonvertToDataCollector

class KonvertProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    @Synchronized
    override fun process(resolver: Resolver): List<KSAnnotated> {
        return withIsolatedConfiguration(environment) {
            val data = collectDataForAnnotatedConverters(resolver)
            val existingKonverter = collectGeneratedKonverter(resolver)
            val newKonverter = data.flatMap { it.toTypeConverters() }
            registerTypeConverters(existingKonverter + newKonverter)

            initCodeBuilder()
            initTypeConverters(resolver)

            generateMappingCode(resolver, data)

            writeFiles()

            generateMetaInfFiles(data)

            emptyList()
        }
    }

    private fun collectGeneratedKonverter(resolver: Resolver): List<TypeConverter> {
        return GeneratedKonverterLoader(resolver, logger).load()
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

    private fun generateMappingCode(resolver: Resolver, converterData: List<AnnotatedConverterData>) {
        converterData.forEach {
            when (it) {
                is KonvertToData -> KonvertToCodeGenerator.generate(it, resolver, logger)
                is KonvertFromData -> KonvertFromCodeGenerator.generate(it, resolver, logger)
                is KonverterData -> KonverterCodeGenerator.generate(it, resolver, logger)
            }
        }
    }

    private fun generateMetaInfFiles(converterData: List<AnnotatedConverterData>) {
        return GeneratedKonverterWriter(codeGenerator, logger).write(converterData)
    }

    private fun registerTypeConverters(typeConverters: List<TypeConverter>) {
        typeConverters
            .groupBy { it.priority }
            .forEach { (priority, list) ->
                TypeConverterRegistry.addConverters(priority, list)
            }
    }

    private fun initTypeConverters(resolver: Resolver) {
        TypeConverterRegistry.initConverters(resolver)
    }

    private fun initCodeBuilder() {
        CodeBuilder.clear()
    }

    private fun collectDataForAnnotatedConverters(resolver: Resolver): List<AnnotatedConverterData> {
        return KonvertToDataCollector.collect(resolver, logger) +
            KonvertFromDataCollector.collect(resolver, logger) +
            KonverterDataCollector.collect(resolver, logger)
    }

}
