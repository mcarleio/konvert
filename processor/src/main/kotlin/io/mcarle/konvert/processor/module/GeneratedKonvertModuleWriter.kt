package io.mcarle.konvert.processor.module

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.konvert.api.GeneratedKonvertModule
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.generatedModuleSuffix
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.konvert.KonverterCodeGenerator
import io.mcarle.konvert.processor.konvert.KonverterData
import io.mcarle.konvert.processor.konvertfrom.KonvertFromCodeGenerator
import io.mcarle.konvert.processor.konvertfrom.KonvertFromData
import io.mcarle.konvert.processor.konvertto.KonvertToCodeGenerator
import io.mcarle.konvert.processor.konvertto.KonvertToData

class GeneratedKonvertModuleWriter(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun write(converterData: List<AnnotatedConverterData>) {
        val konverterFqns = converterData.extractGeneratedKonverterFqns()
        val konvertToFqns = converterData.extractGeneratedKonvertToFqns()
        val konvertFromFqns = converterData.extractGeneratedKonvertFromFqns()

        if (konverterFqns.isEmpty() && konvertToFqns.isEmpty() && konvertFromFqns.isEmpty()) {
            return
        }

        buildGeneratedModule(konverterFqns, konvertToFqns, konvertFromFqns)
    }

    private fun buildGeneratedModule(konverterFqns: Set<String>, konvertToFqns: Set<String>, konvertFromFqns: Set<String>) {
        val cb = CodeBuilder.getOrCreate(
            GENERATED_KONVERTER_MODULE_PACKAGE,
            GENERATED_KONVERTER_MODULE_FILENAME,
            ""
        ) {
            logger.logging("Generating interface $GENERATED_KONVERTER_MODULE_FILENAME${Configuration.generatedModuleSuffix}...")
            TypeSpec.interfaceBuilder("$GENERATED_KONVERTER_MODULE_FILENAME${Configuration.generatedModuleSuffix}")
                .addAnnotation(
                    AnnotationSpec.builder(GeneratedKonvertModule::class)
                        .addGeneratedKonverterFqns(konverterFqns)
                        .addGeneratedKonvertToFqns(konvertToFqns)
                        .addGeneratedKonvertFromFqns(konvertFromFqns)
                        .build()
                )
        }
        cb.build().writeTo(codeGenerator, true, emptyList())
    }

    private fun List<AnnotatedConverterData>.extractGeneratedKonverterFqns(): Set<String> {
        return this.filterIsInstance<KonverterData>().flatMap {
            KonverterCodeGenerator.toFunctionFullyQualifiedNames(it)
        }.toSet()
    }

    private fun List<AnnotatedConverterData>.extractGeneratedKonvertToFqns(): Set<String> {
        return this.filterIsInstance<KonvertToData>().flatMap {
            KonvertToCodeGenerator.toFunctionFullyQualifiedNames(it)
        }.toSet()
    }

    private fun List<AnnotatedConverterData>.extractGeneratedKonvertFromFqns(): Set<String> {
        return this.filterIsInstance<KonvertFromData>().flatMap {
            KonvertFromCodeGenerator.toFunctionFullyQualifiedNames(it)
        }.toSet()
    }

    private fun AnnotationSpec.Builder.addGeneratedKonverterFqns(
        fqns: Set<String>
    ): AnnotationSpec.Builder {
        logger.logging("Register @${Konverter::class.simpleName} functions: $fqns")

        return this.addMember(
            "${GeneratedKonvertModule::konverterFQN.name} = %L",
            fqns.toAnnotationArray()
        )
    }

    private fun AnnotationSpec.Builder.addGeneratedKonvertToFqns(
        fqns: Set<String>
    ): AnnotationSpec.Builder {
        logger.logging("Register @${KonvertTo::class.simpleName} functions: $fqns")

        return this.addMember(
            "${GeneratedKonvertModule::konvertToFQN.name} = %L",
            fqns.toAnnotationArray()
        )
    }

    private fun AnnotationSpec.Builder.addGeneratedKonvertFromFqns(
        fqns: Set<String>
    ): AnnotationSpec.Builder {
        logger.logging("Register @${KonvertFrom::class.simpleName} functions: $fqns")

        return this.addMember(
            "${GeneratedKonvertModule::konvertFromFQN.name} = %L",
            fqns.toAnnotationArray()
        )
    }

    private fun Set<String>.toAnnotationArray(): String {
        return this.joinToString(prefix = "[", separator = ", ", postfix = "]") { "\"$it\"" }
    }

}
