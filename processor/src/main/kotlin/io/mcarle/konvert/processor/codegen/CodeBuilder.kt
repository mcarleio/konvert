package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.GeneratedKonverter
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.addGeneratedKonverterAnnotation
import io.mcarle.konvert.converter.api.config.generatedFilenameSuffix

class CodeBuilder private constructor(
    private val builder: FileSpec.Builder,
    private val typeBuilder: TypeSpec.Builder?,
    val originating: MutableSet<KSFile>
) {
    fun addFunction(funBuilder: FunSpec.Builder, priority: Priority, toType: Boolean = false, originating: KSFile?) {
        addFunction(
            funSpec = funBuilder.apply {
                if (Configuration.addGeneratedKonverterAnnotation
                    && this.parameters.size <= 1 // do not add annotation to functions with multiple parameters
                ) {
                    addAnnotation(
                        AnnotationSpec.builder(GeneratedKonverter::class)
                            .addMember("${GeneratedKonverter::priority.name} = %L", priority)
                            .build()
                    )
                }
            }.build(),
            toType = toType,
            originating = originating
        )
    }

    private fun addFunction(funSpec: FunSpec, toType: Boolean, originating: KSFile?) {
        if (toType) {
            typeBuilder?.addFunction(funSpec)
        } else {
            builder.addFunction(funSpec)
        }
        if (originating != null) this.originating += originating
    }

    fun addImport(type: KSType, alias: String) {
        builder.addAliasedImport(type.toClassName(), alias)
    }

    fun build(): FileSpec {
        if (typeBuilder != null) {
            builder.addType(typeBuilder.build())
        }
        return builder.build()
    }

    companion object {

        private val cache = mutableMapOf<QualifiedName, CodeBuilder>()

        fun all(): Iterable<CodeBuilder> = cache.values

        fun clear() {
            cache.clear()
        }

        fun getOrCreate(packageName: String, fileName: String, typeBuilderProvider: () -> TypeSpec.Builder? = { null }): CodeBuilder {
            val qualifiedName = QualifiedName(packageName, fileName)
            return cache[qualifiedName] ?: CodeBuilder(
                builder = FileSpec.builder(qualifiedName.packageName, qualifiedName.fileName + Configuration.generatedFilenameSuffix),
                typeBuilder = typeBuilderProvider.invoke(),
                originating = mutableSetOf(),
            ).also {
                cache[qualifiedName] = it
            }
        }

        private data class QualifiedName(
            val packageName: String,
            val fileName: String
        )
    }
}
