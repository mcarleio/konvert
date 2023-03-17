package io.mcarle.lib.kmapper.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.converter.api.classDeclaration
import io.mcarle.lib.kmapper.converter.api.isNullable

@AutoService(TypeConverter::class)
class MapToMapConverter : AbstractTypeConverter() {

    companion object {
        private val MAP = "kotlin.collections.Map"
        private val MUTABLEMAP = "kotlin.collections.MutableMap"

        private val HASHMAP = "java.util.HashMap" // not "kotlin.collections.HashMap"

        private val LINKEDHASHMAP = "java.util.LinkedHashMap" // not "kotlin.collections.LinkedHashMap"
    }

    private val mapType: KSType by lazy {
        resolver.getClassDeclarationByName<Map<*, *>>()!!.asStarProjectedType()
    }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            mapType.isAssignableFrom(sourceNotNullable)
                    && mapType.isAssignableFrom(targetNotNullable)
        } && TypeConverterRegistry.any {
            it.matches(
                source = source.arguments[0].type!!.resolve(),
                target = target.arguments[0].type!!.resolve(),
            )
        } && TypeConverterRegistry.any {
            it.matches(
                source = source.arguments[1].type!!.resolve(),
                target = target.arguments[1].type!!.resolve(),
            )
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val genericTargetKeyVariance = target.arguments[0].variance.let {
            if (it == Variance.INVARIANT) {
                target.declaration.typeParameters[0].variance
            } else {
                it
            }
        }
        val genericTargetValueVariance = target.arguments[1].variance.let {
            if (it == Variance.INVARIANT) {
                target.declaration.typeParameters[1].variance
            } else {
                it
            }
        }

        val genericSourceKeyType = source.arguments[0].type!!.resolve()
        val genericSourceValueType = source.arguments[1].type!!.resolve()
        val genericTargetKeyType = target.arguments[0].type!!.resolve()
        val genericTargetValueType = target.arguments[1].type!!.resolve()
        val keyTypeConverter = TypeConverterRegistry.firstOrNull {
            it.matches(
                source = genericSourceKeyType,
                target = genericTargetKeyType,
            )
        }!!
        val valueTypeConverter = TypeConverterRegistry.firstOrNull {
            it.matches(
                source = genericSourceValueType,
                target = genericTargetValueType,
            )
        }!!
        val nc = if (source.isNullable()) "?" else ""

        var mapTypeChanged = false
        var mappedToListOfPairs = false
        var castNeeded = false

        val mapSourceContentCode = when {
            genericSourceKeyType == genericTargetKeyType -> when {
                genericSourceValueType == genericTargetValueType -> fieldName
                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapValues·{·it.value!!·}"
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        castNeeded = true
                    }
                    fieldName
                }

                else -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapValues·{·(_,·it)·-> ${
                        valueTypeConverter.convert(
                            "it",
                            genericSourceValueType,
                            genericTargetValueType
                        )
                    } }"
                }
            }

            needsNotNullAssertionOperator(genericSourceKeyType, genericTargetKeyType) -> when {
                genericSourceValueType == genericTargetValueType -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·it.key!!·}"
                }

                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    mappedToListOfPairs = true
                    "$fieldName$nc.map·{·it.key!!·to·it.value!!·}"
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        castNeeded = true
                    }
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·it.key!!·}"
                }

                else -> {
                    mapTypeChanged = true
                    mappedToListOfPairs = true
                    """
$fieldName$nc.map·{·(key,·value)·->
⇥val·newKey·=·key!!
val·newValue·=·${valueTypeConverter.convert("value", genericSourceValueType, genericTargetValueType)}
newKey·to·newValue
⇤}
                    """.trimIndent()
                }
            }

            genericSourceKeyType == genericTargetKeyType.makeNotNullable() -> {
                if (genericTargetKeyVariance == Variance.INVARIANT) {
                    castNeeded = true
                }
                when {
                    genericSourceValueType == genericTargetValueType -> fieldName
                    needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                        mapTypeChanged = true
                        "$fieldName$nc.mapValues·{·it.value!!·}"
                    }

                    genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                        if (genericTargetValueVariance == Variance.INVARIANT) {
                            castNeeded = true
                        }
                        fieldName
                    }

                    else -> {
                        mapTypeChanged = true
                        "$fieldName$nc.mapValues·{·(_,·it)·-> ${
                            valueTypeConverter.convert(
                                "it",
                                genericSourceValueType,
                                genericTargetValueType
                            )
                        } }"
                    }
                }
            }

            else -> when {
                genericSourceValueType == genericTargetValueType -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·(it,·_)·-> ${
                        keyTypeConverter.convert(
                            "it",
                            genericSourceKeyType,
                            genericTargetKeyType
                        )
                    } }"
                }

                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    mappedToListOfPairs = true
                    """
$fieldName$nc.map·{·(key,·value)·-> 
⇥val·newKey·=·${keyTypeConverter.convert("key", genericSourceKeyType, genericTargetKeyType)}
val·newValue·=·value!!
newKey·to·newValue
⇤}
                    """.trimIndent()
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        castNeeded = true
                    }
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·(it,·_)·-> ${
                        keyTypeConverter.convert("it", genericSourceKeyType, genericTargetKeyType)
                    } }"
                }

                else -> {
                    mapTypeChanged = true
                    mappedToListOfPairs = true
                    """
$fieldName$nc.map·{·(key,·value)·-> 
⇥val·newKey·=·${keyTypeConverter.convert("key", genericSourceKeyType, genericTargetKeyType)}
val·newValue·=·${valueTypeConverter.convert("value", genericSourceValueType, genericTargetValueType)}
newKey·to·newValue
⇤}
                    """.trimIndent()
                }
            }
        }

        val mapSourceContainerCode = when {
            target.isExactly(MAP) -> if (mappedToListOfPairs) "$nc.toMap()" else ""
            target.isExactly(MUTABLEMAP) -> if (!mapTypeChanged && source.isInstanceOf(MUTABLEMAP)) "" else if (mappedToListOfPairs) "$nc.toMap(kotlin.collections.LinkedHashMap())" else "$nc.toMutableMap()"
            target.isExactly(HASHMAP) -> if (!mapTypeChanged && source.isInstanceOf(HASHMAP)) "" else "$nc.toMap(kotlin.collections.HashMap())"
            target.isExactly(LINKEDHASHMAP) -> if (!mapTypeChanged && source.isInstanceOf(LINKEDHASHMAP)) "" else "$nc.toMap(kotlin.collections.LinkedHashMap())"

            else -> throw UnsupportedTargetMapException(target)
        }

        val code = mapSourceContentCode + mapSourceContainerCode + appendNotNullAssertionOperatorIfNeeded(source, target)

        return if (castNeeded) {
            "($code·as·$target)" // encapsulate with braces
        } else {
            code
        }
    }


    private fun KSType.isExactly(qualifiedName: String): Boolean {
        return this.classDeclaration() == resolver.getClassDeclarationByName(qualifiedName)
    }

    private fun KSType.isInstanceOf(qualifiedName: String): Boolean {
        return resolver.getClassDeclarationByName(qualifiedName)!!.asStarProjectedType()
            .isAssignableFrom(this.starProjection().makeNotNullable())
    }

}

class UnsupportedTargetMapException(type: KSType) : RuntimeException(
    "Maps of $type are not supported as target by ${MapToMapConverter::class.simpleName}"
)