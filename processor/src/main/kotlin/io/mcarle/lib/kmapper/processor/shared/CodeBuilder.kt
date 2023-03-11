package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

class CodeBuilder private constructor(
    private val builder: FileSpec.Builder,
    private val typeBuilder: TypeSpec.Builder?,
    val originating: MutableSet<KSFile>
) {
    fun addFunction(funSpec: FunSpec, toType: Boolean = false, originating: KSFile?) {
        if (toType) {
            typeBuilder?.addFunction(funSpec)
        } else {
            builder.addFunction(funSpec)
        }
        if (originating != null) this.originating += originating
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
                builder = FileSpec.builder(qualifiedName.packageName, qualifiedName.fileName + "KMap"),
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