package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

@AutoService(TypeConverter::class)
class IterableToIterableConverter : AbstractTypeConverter() {

    companion object {
        private val ITERABLE = "kotlin.collections.Iterable"
        private val MUTABLEITERABLE = "kotlin.collections.MutableIterable"
        private val COLLECTION = "kotlin.collections.Collection"
        private val MUTABLECOLLECTION = "kotlin.collections.MutableCollection"
        private val LIST = "kotlin.collections.List"
        private val MUTABLELIST = "kotlin.collections.MutableList"
        private val ARRAYLIST = "java.util.ArrayList"
        private val SET = "kotlin.collections.Set"
        private val MUTABLESET = "kotlin.collections.MutableSet"
        private val HASHSET = "java.util.HashSet"
        private val LINKEDHASHSET = "java.util.LinkedHashSet"
        fun supported() = listOf(
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
        )
    }

    private val iterableType: KSType by lazy { resolver.builtIns.iterableType }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            iterableType.isAssignableFrom(sourceNotNullable) &&
                iterableType.isAssignableFrom(targetNotNullable)
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
        var mapCodeBlock: CodeBlock? = null

        val mapSourceContentCode = when {
            genericSourceType == genericTargetType -> fieldName
            needsNotNullAssertionOperator(genericSourceType, genericTargetType) -> {
                listTypeChanged = true
                "$fieldName$nc.map·{·it!!·}"
            }

            genericSourceType == genericTargetType.makeNotNullable() -> {
                if (genericTargetVariance == Variance.INVARIANT) {
                    castNeeded = true
                }
                fieldName
            }

            else -> {
                listTypeChanged = true
                mapCodeBlock = typeConverter.convert("it", genericSourceType, genericTargetType)
                "$fieldName$nc.map·{ %L }"
            }
        }

        val mapSourceContainerCode = when {
            target.isExactly(ITERABLE) -> ""
            target.isExactly(MUTABLEITERABLE) -> if (!listTypeChanged && source.isInstanceOf(MUTABLEITERABLE)) "" else "$nc.toMutableList()"
            target.isExactly(COLLECTION) -> if (listTypeChanged || source.isInstanceOf(COLLECTION)) "" else "$nc.toList()"
            target.isExactly(MUTABLECOLLECTION) -> if (!listTypeChanged && source.isInstanceOf(MUTABLECOLLECTION)) "" else "$nc.toMutableList()"
            target.isExactly(LIST) -> if (listTypeChanged || source.isInstanceOf(LIST)) "" else "$nc.toList()"
            target.isExactly(MUTABLELIST) -> if (!listTypeChanged && source.isInstanceOf(MUTABLELIST)) "" else "$nc.toMutableList()"
            target.isExactly(ARRAYLIST) -> if (!listTypeChanged && source.isInstanceOf(ARRAYLIST)) "" else "$nc.toCollection(kotlin.collections.ArrayList())"
            target.isExactly(SET) -> if (!listTypeChanged && source.isInstanceOf(SET)) "" else "$nc.toSet()"
            target.isExactly(MUTABLESET) -> if (!listTypeChanged && source.isInstanceOf(MUTABLESET)) "" else "$nc.toMutableSet()"
            target.isExactly(HASHSET) -> if (!listTypeChanged && source.isInstanceOf(HASHSET)) "" else "$nc.toCollection(kotlin.collections.HashSet())"
            target.isExactly(LINKEDHASHSET) -> if (!listTypeChanged && source.isInstanceOf(LINKEDHASHSET)) "" else "$nc.toCollection(kotlin.collections.LinkedHashSet())"

            else -> throw UnsupportedTargetIterableException(target)
        }


        val code = mapSourceContentCode + mapSourceContainerCode + appendNotNullAssertionOperatorIfNeeded(source, target)

        return CodeBlock.of(
            if (castNeeded) {
                "($code·as·$target)" // encapsulate with braces
            } else {
                code
            },
            *listOfNotNull(mapCodeBlock).toTypedArray()
        )
    }

    private fun KSType.isExactly(qualifiedName: String): Boolean {
        return this.classDeclaration() == resolver.getClassDeclarationByName(qualifiedName)
    }

    private fun KSType.isInstanceOf(qualifiedName: String): Boolean {
        return resolver.getClassDeclarationByName(qualifiedName)!!.asStarProjectedType()
            .isAssignableFrom(this.starProjection().makeNotNullable())
    }

}

class UnsupportedTargetIterableException(type: KSType) : RuntimeException(
    "Iterables of $type are not supported as target by ${IterableToIterableConverter::class.simpleName}"
)
