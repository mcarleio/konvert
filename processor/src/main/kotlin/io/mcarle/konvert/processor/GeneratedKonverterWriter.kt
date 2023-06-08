package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.konvert.KonverterCodeGenerator
import io.mcarle.konvert.processor.konvert.KonverterData
import io.mcarle.konvert.processor.konvertfrom.KonvertFromCodeGenerator
import io.mcarle.konvert.processor.konvertfrom.KonvertFromData
import io.mcarle.konvert.processor.konvertto.KonvertToCodeGenerator
import io.mcarle.konvert.processor.konvertto.KonvertToData

class GeneratedKonverterWriter(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun write(converterData: List<AnnotatedConverterData>) {
        writeMetaInfFile(
            lines = converterData.filterIsInstance<KonverterData>().flatMap {
                KonverterCodeGenerator.toFunctionFullyQualifiedNames(it)
            }.toSet(),
            konverterType = Konvert::class.java.simpleName
        )
        writeMetaInfFile(
            lines = converterData.filterIsInstance<KonvertToData>().flatMap {
                KonvertToCodeGenerator.toFunctionFullyQualifiedNames(it)
            }.toSet(),
            konverterType = KonvertTo::class.java.simpleName
        )
        writeMetaInfFile(
            lines = converterData.filterIsInstance<KonvertFromData>().flatMap {
                KonvertFromCodeGenerator.toFunctionFullyQualifiedNames(it)
            }.toSet(),
            konverterType = KonvertFrom::class.java.simpleName
        )
    }

    private fun writeMetaInfFile(lines: Set<String>, konverterType: String) {
        if (lines.isEmpty()) return
        val output = codeGenerator.createNewFile(
            dependencies = Dependencies.ALL_FILES,
            packageName = "META-INF/konvert",
            fileName = "io.mcarle.konvert.api",
            extensionName = konverterType
        )
        output.bufferedWriter().use { writer ->
            lines.forEach { writer.write(it + "\n") }
        }
    }

}
