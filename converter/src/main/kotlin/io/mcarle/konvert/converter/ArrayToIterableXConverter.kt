package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

abstract class ArrayToIterableXConverter(val targetIterableFQN: String) : BaseArrayConverter() {

    companion object {
        internal fun supported() = listOf(
            ITERABLE,
            MUTABLEITERABLE,
            COLLECTION,
            MUTABLECOLLECTION,
            LIST,
            MUTABLELIST,
            ARRAYLIST,
            SET,
            MUTABLESET,
            HASHSET,
            LINKEDHASHSET,
            IMMUTABLE_COLLECTION,
            IMMUTABLE_LIST,
            IMMUTABLE_SET,
            PERSISTENT_COLLECTION,
            PERSISTENT_LIST,
            PERSISTENT_SET,
        )
    }

    private val targetClassDeclaration: KSClassDeclaration? by lazy { resolver.getClassDeclarationByName(targetIterableFQN) }
    private val targetType: KSType? by lazy { targetClassDeclaration?.asStarProjectedType() }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        if (targetType == null) return false

        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            val sourceArrayElementType = sourceArrayElementType(sourceNotNullable)?.first

            sourceArrayElementType != null
                && targetType!!.isAssignableFrom(targetNotNullable)
                && targetNotNullable.isExactlyTarget()
                && TypeConverterRegistry.any {
                it.matches(
                    source = sourceArrayElementType,
                    target = target.arguments[0].type?.resolve() ?: resolver.builtIns.anyType,
                )
            }
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val sourceNotNullable = source.makeNotNullable()
        val genericSourceType = sourceArrayElementType(sourceNotNullable)?.first!!
        val genericTargetType = target.arguments[0].type?.resolve() ?: resolver.builtIns.anyType

        val typeConverter = TypeConverterRegistry.first {
            it.matches(
                source = genericSourceType,
                target = genericTargetType,
            )
        }

        val nc = if (source.isNullable()) "?" else ""
        val conversionToListCodeBlock = applyNotNullEnforcementIfNeeded(
            CodeBlock.of(
                "$fieldName$nc.map·{ %L }",
                typeConverter.convert("it", genericSourceType, genericTargetType)
            ),
            fieldName,
            source,
            target
        )


        return CodeBlock.of("%L%L", conversionToListCodeBlock, convertArray(nc))
    }

    abstract fun convertArray(nc: String): CodeBlock

    private fun KSType.isExactlyTarget(): Boolean {
        return this.classDeclaration() == targetClassDeclaration
    }

}


@AutoService(TypeConverter::class)
class ArrayToIterableConverter : ArrayToIterableXConverter(ITERABLE) {
    override fun convertArray(nc: String): CodeBlock = CodeBlock.of("")
}

@AutoService(TypeConverter::class)
class ArrayToMutableIterableConverter : ArrayToIterableXConverter(MUTABLEITERABLE) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class ArrayToCollectionConverter : ArrayToIterableXConverter(COLLECTION) {
    override fun convertArray(nc: String): CodeBlock = CodeBlock.of("")
}

@AutoService(TypeConverter::class)
class ArrayToMutableCollectionConverter : ArrayToIterableXConverter(MUTABLECOLLECTION) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class ArrayToListConverter : ArrayToIterableXConverter(LIST) {
    override fun convertArray(nc: String): CodeBlock = CodeBlock.of("")
}

@AutoService(TypeConverter::class)
class ArrayToMutableListConverter : ArrayToIterableXConverter(MUTABLELIST) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class ArrayToArrayListConverter : ArrayToIterableXConverter(ARRAYLIST) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.ArrayList())")
    }
}

@AutoService(TypeConverter::class)
class ArrayToSetConverter : ArrayToIterableXConverter(SET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toSet()")
    }
}

@AutoService(TypeConverter::class)
class ArrayToMutableSetConverter : ArrayToIterableXConverter(MUTABLESET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableSet()")
    }
}

@AutoService(TypeConverter::class)
class ArrayToHashSetConverter : ArrayToIterableXConverter(HASHSET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.HashSet())")
    }
}

@AutoService(TypeConverter::class)
class ArrayToLinkedHashSetConverter : ArrayToIterableXConverter(LINKEDHASHSET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.LinkedHashSet())")
    }
}

@AutoService(TypeConverter::class)
class ArrayToImmutableCollectionConverter : ArrayToIterableXConverter(IMMUTABLE_COLLECTION) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableList"))
    }
}

@AutoService(TypeConverter::class)
class ArrayToImmutableListConverter : ArrayToIterableXConverter(IMMUTABLE_LIST) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableList"))
    }
}

@AutoService(TypeConverter::class)
class ArrayToImmutableSetConverter : ArrayToIterableXConverter(IMMUTABLE_SET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableSet"))
    }
}

@AutoService(TypeConverter::class)
class ArrayToPersistentCollectionConverter : ArrayToIterableXConverter(PERSISTENT_COLLECTION) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentList"))
    }
}

@AutoService(TypeConverter::class)
class ArrayToPersistentListConverter : ArrayToIterableXConverter(PERSISTENT_LIST) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentList"))
    }
}

@AutoService(TypeConverter::class)
class ArrayToPersistentSetConverter : ArrayToIterableXConverter(PERSISTENT_SET) {
    override fun convertArray(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentSet"))
    }
}
