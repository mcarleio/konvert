package io.mcarle.konvert.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Variance
import io.mcarle.konvert.converter.api.AbstractTypeConverter

internal const val BOOLEAN_ARRAY = "kotlin.BooleanArray"
internal const val DOUBLE_ARRAY = "kotlin.DoubleArray"
internal const val FLOAT_ARRAY = "kotlin.FloatArray"
internal const val LONG_ARRAY = "kotlin.LongArray"
internal const val INT_ARRAY = "kotlin.IntArray"
internal const val SHORT_ARRAY = "kotlin.ShortArray"
internal const val BYTE_ARRAY = "kotlin.ByteArray"
internal const val CHAR_ARRAY = "kotlin.CharArray"

abstract class BaseArrayConverter : AbstractTypeConverter() {

    protected val arrayType: KSType by lazy { resolver.builtIns.arrayType }
    protected val primitiveArrayTypes by lazy {
        @Suppress("UNCHECKED_CAST")
        mapOf(
            resolver.getClassDeclarationByName(BOOLEAN_ARRAY)?.asStarProjectedType() to resolver.builtIns.booleanType,
            resolver.getClassDeclarationByName(DOUBLE_ARRAY)?.asStarProjectedType() to resolver.builtIns.doubleType,
            resolver.getClassDeclarationByName(FLOAT_ARRAY)?.asStarProjectedType() to resolver.builtIns.floatType,
            resolver.getClassDeclarationByName(LONG_ARRAY)?.asStarProjectedType() to resolver.builtIns.longType,
            resolver.getClassDeclarationByName(INT_ARRAY)?.asStarProjectedType() to resolver.builtIns.intType,
            resolver.getClassDeclarationByName(SHORT_ARRAY)?.asStarProjectedType() to resolver.builtIns.shortType,
            resolver.getClassDeclarationByName(BYTE_ARRAY)?.asStarProjectedType() to resolver.builtIns.byteType,
            resolver.getClassDeclarationByName(CHAR_ARRAY)?.asStarProjectedType() to resolver.builtIns.charType,
        ).filterKeys { it != null } as Map<KSType, KSType>
    }

    protected fun extractGenericSource(
        sourceParameter: KSTypeArgument,
    ): Pair<KSType, Variance> {
        val genericSourceVariance = sourceParameter.variance
        return when (genericSourceVariance) {
            Variance.STAR -> resolver.builtIns.anyType // "*"
            Variance.INVARIANT, /* "" */
            Variance.COVARIANT /* "out" */ -> sourceParameter.type?.resolve() ?: resolver.builtIns.anyType
            Variance.CONTRAVARIANT /* "in" */ -> resolver.builtIns.anyType
        } to genericSourceVariance
    }

    protected fun extractGenericTarget(
        targetParameter: KSTypeArgument,
    ): Pair<KSType, Variance> {
        val genericTargetVariance = targetParameter.variance
        return when (genericTargetVariance) {
            Variance.STAR -> resolver.builtIns.anyType // "*"
            Variance.INVARIANT, /* "" */
            Variance.COVARIANT, /* "out" */
            Variance.CONTRAVARIANT /* "in" */ -> targetParameter.type?.resolve() ?: resolver.builtIns.anyType
        } to genericTargetVariance
    }

    protected fun targetArrayElementType(targetNotNullable: KSType): Triple<KSType, Variance, Boolean>? {
        return if (arrayType.isAssignableFrom(targetNotNullable)) {
            extractGenericTarget(targetNotNullable.arguments[0]).let {
                Triple(it.first, it.second, false)
            }
        } else {
            primitiveArrayTypes
                .entries
                .firstOrNull { it.key.isAssignableFrom(targetNotNullable) }
                ?.let { Triple(it.value, Variance.INVARIANT, true) }
        }
    }

    protected fun sourceArrayElementType(sourceNotNullable: KSType): Triple<KSType, Variance, Boolean>? {
        return if (arrayType.isAssignableFrom(sourceNotNullable)) {
            extractGenericSource(sourceNotNullable.arguments[0]).let {
                Triple(it.first, it.second, false)
            }
        } else {
            primitiveArrayTypes
                .entries
                .firstOrNull { it.key.isAssignableFrom(sourceNotNullable) }
                ?.let { Triple(it.value, Variance.INVARIANT, true) }
        }
    }

}
