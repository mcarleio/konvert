package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.*

internal const val ITERABLE = "kotlin.collections.Iterable"
internal const val MUTABLEITERABLE = "kotlin.collections.MutableIterable"
internal const val COLLECTION = "kotlin.collections.Collection"
internal const val MUTABLECOLLECTION = "kotlin.collections.MutableCollection"
internal const val LIST = "kotlin.collections.List"
internal const val MUTABLELIST = "kotlin.collections.MutableList"
internal const val ARRAYLIST = "java.util.ArrayList"
internal const val SET = "kotlin.collections.Set"
internal const val MUTABLESET = "kotlin.collections.MutableSet"
internal const val HASHSET = "java.util.HashSet"
internal const val LINKEDHASHSET = "java.util.LinkedHashSet"

internal const val KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE = "kotlinx.collections.immutable"
internal const val IMMUTABLE_COLLECTION = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.ImmutableCollection"
internal const val IMMUTABLE_LIST = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.ImmutableList"
internal const val IMMUTABLE_SET = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.ImmutableSet"
internal const val PERSISTENT_COLLECTION = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.PersistentCollection"
internal const val PERSISTENT_LIST = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.PersistentList"
internal const val PERSISTENT_SET = "$KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE.PersistentSet"

abstract class IterableToXConverter(
    val targetFQN: String
) : AbstractTypeConverter() {

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

    private val iterableType: KSType by lazy { resolver.builtIns.iterableType }

    private val targetClassDeclaration: KSClassDeclaration? by lazy { resolver.getClassDeclarationByName(targetFQN) }

    private val targetType: KSType? by lazy { targetClassDeclaration?.asStarProjectedType() }


    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        if (targetType == null) return false

        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            iterableType.isAssignableFrom(sourceNotNullable) &&
                targetType!!.isAssignableFrom(targetNotNullable) && targetNotNullable.isExactlyTarget()
        } && TypeConverterRegistry.any {
            it.matches(
                source = source.arguments[0].type!!.resolve(),
                target = target.arguments[0].type!!.resolve(),
            )
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val genericTargetVariance = target.arguments[0].variance.let {
            if (it == Variance.INVARIANT) {
                target.declaration.typeParameters[0].variance
            } else {
                it
            }
        }
        val genericSourceType = source.arguments[0].type!!.resolve()
        val genericTargetType = target.arguments[0].type!!.resolve()
        val typeConverter = TypeConverterRegistry.first {
            it.matches(
                source = genericSourceType,
                target = genericTargetType,
            )
        }
        val nc = if (source.isNullable()) "?" else ""
        var listTypeChanged = false
        var castNeeded = false

        val args = mutableListOf<Any>()

        val mapSourceContentCode = when (genericSourceType) {
            genericTargetType -> fieldName
            genericTargetType.makeNotNullable() -> {
                if (genericTargetVariance == Variance.INVARIANT) {
                    castNeeded = true
                }
                fieldName
            }
            else -> {
                listTypeChanged = true
                args += typeConverter.convert("it", genericSourceType, genericTargetType)
                "$fieldName$nc.map·{ %L }"
            }
        }

        val mapSourceContainerCode = if (matchesTarget(listTypeChanged, source)) {
            CodeBlock.of("")
        } else {
            this.convertIterable(nc)
        }
        args += mapSourceContainerCode

        val code = mapSourceContentCode + "%L" + appendNotNullAssertionOperatorIfNeeded(source, target)

        return CodeBlock.of(
            if (castNeeded) {
                args += target.toTypeName()
                "($code·as·%T)" // encapsulate with braces
            } else {
                code
            },
            *args.toTypedArray()
        )
    }

    private fun KSType.isExactlyTarget(): Boolean {
        return this.classDeclaration() == targetClassDeclaration
    }

    protected fun KSType.isInstanceOfTarget(): Boolean {
        return targetType!!.isAssignableFrom(this.starProjection().makeNotNullable())
    }

    abstract fun convertIterable(nc: String): CodeBlock

    open fun matchesTarget(listTypeChanged: Boolean, source: KSType): Boolean {
        return !listTypeChanged && source.isInstanceOfTarget()
    }


}

@AutoService(TypeConverter::class)
class IterableToIterableConverter : IterableToXConverter(ITERABLE) {
    override fun convertIterable(nc: String): CodeBlock = CodeBlock.of("")
}

@AutoService(TypeConverter::class)
class IterableToMutableIterableConverter : IterableToXConverter(MUTABLEITERABLE) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class IterableToCollectionConverter : IterableToXConverter(COLLECTION) {
    override fun matchesTarget(listTypeChanged: Boolean, source: KSType): Boolean {
        return listTypeChanged || source.isInstanceOfTarget()
    }

    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toList()")
    }
}

@AutoService(TypeConverter::class)
class IterableToMutableCollectionConverter : IterableToXConverter(MUTABLECOLLECTION) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class IterableToListConverter : IterableToXConverter(LIST) {
    override fun matchesTarget(listTypeChanged: Boolean, source: KSType): Boolean {
        return listTypeChanged || source.isInstanceOfTarget()
    }

    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toList()")
    }
}

@AutoService(TypeConverter::class)
class IterableToMutableListConverter : IterableToXConverter(MUTABLELIST) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableList()")
    }
}

@AutoService(TypeConverter::class)
class IterableToArrayListConverter : IterableToXConverter(ARRAYLIST) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.ArrayList())")
    }
}

@AutoService(TypeConverter::class)
class IterableToSetConverter : IterableToXConverter(SET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toSet()")
    }
}

@AutoService(TypeConverter::class)
class IterableToMutableSetConverter : IterableToXConverter(MUTABLESET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMutableSet()")
    }
}

@AutoService(TypeConverter::class)
class IterableToHashSetConverter : IterableToXConverter(HASHSET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.HashSet())")
    }
}

@AutoService(TypeConverter::class)
class IterableToLinkedHashSetConverter : IterableToXConverter(LINKEDHASHSET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toCollection(kotlin.collections.LinkedHashSet())")
    }
}

@AutoService(TypeConverter::class)
class IterableToImmutableCollectionConverter : IterableToXConverter(IMMUTABLE_COLLECTION) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableList"))
    }
}

@AutoService(TypeConverter::class)
class IterableToImmutableListConverter : IterableToXConverter(IMMUTABLE_LIST) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableList"))
    }
}

@AutoService(TypeConverter::class)
class IterableToImmutableSetConverter : IterableToXConverter(IMMUTABLE_SET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toImmutableSet"))
    }
}

@AutoService(TypeConverter::class)
class IterableToPersistentCollectionConverter : IterableToXConverter(PERSISTENT_COLLECTION) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentList"))
    }
}

@AutoService(TypeConverter::class)
class IterableToPersistentListConverter : IterableToXConverter(PERSISTENT_LIST) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentList"))
    }
}

@AutoService(TypeConverter::class)
class IterableToPersistentSetConverter : IterableToXConverter(PERSISTENT_SET) {
    override fun convertIterable(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName(KOTLINX_COLLECTIONS_IMMUTABLE_PACKAGE, "toPersistentSet"))
    }
}
