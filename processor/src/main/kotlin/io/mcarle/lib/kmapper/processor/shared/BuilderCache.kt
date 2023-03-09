package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

object BuilderCache {

    private val x = mutableMapOf<QualifiedName, CodeBuilder>()

    fun getOrPut(qualifiedName: QualifiedName, defaultValue: () -> CodeBuilder): CodeBuilder {
        return x[qualifiedName] ?: defaultValue().also {
            x[qualifiedName] = it
        }
    }

    fun all(): Iterable<CodeBuilder> = x.values

    fun clear() {
        x.clear()
    }

}

data class QualifiedName(
    val packageName: String,
    val fileName: String
)

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
        fun create(qualifiedName: QualifiedName, typeBuilder: TypeSpec.Builder? = null) = CodeBuilder(
            builder = FileSpec.builder(qualifiedName.packageName, qualifiedName.fileName + "KMap"),
            typeBuilder = typeBuilder,
            originating = mutableSetOf(),
        )
    }
}