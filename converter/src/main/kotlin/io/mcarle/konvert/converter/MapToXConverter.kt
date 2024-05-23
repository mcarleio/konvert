package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

internal const val KOTLIN_COLLECTIONS_PACKAGE = "kotlin.collections"

internal const val MAP = "$KOTLIN_COLLECTIONS_PACKAGE.Map"
internal const val MUTABLEMAP = "$KOTLIN_COLLECTIONS_PACKAGE.MutableMap"
internal const val JAVA_HASHMAP = "java.util.HashMap" // not "kotlin.collections.HashMap"
internal const val JAVA_LINKEDHASHMAP = "java.util.LinkedHashMap" // not "kotlin.collections.LinkedHashMap"
internal const val HASHMAP = "$KOTLIN_COLLECTIONS_PACKAGE.HashMap"
internal const val LINKEDHASHMAP = "$KOTLIN_COLLECTIONS_PACKAGE.LinkedHashMap"
internal const val PERISTENT_MAP = "kotlinx.collections.immutable.PersistentMap"
internal const val IMMUTABLE_MAP = "kotlinx.collections.immutable.ImmutableMap"

abstract class MapToXConverter(
    val targetFQN: String,
    private val alternativeFQN: String? = null
) : AbstractTypeConverter() {

    companion object {
        internal fun supported() = listOf(
            MAP,
            MUTABLEMAP,
            JAVA_HASHMAP,
            JAVA_LINKEDHASHMAP,
            HASHMAP,
            LINKEDHASHMAP,
            PERISTENT_MAP,
            IMMUTABLE_MAP,
        )
    }

    private val mapType: KSType by lazy {
        resolver.getClassDeclarationByName<Map<*, *>>()!!.asStarProjectedType()
    }

    private val targetClassDeclaration: KSClassDeclaration? by lazy {
        resolver.getClassDeclarationByName(targetFQN)
            ?: alternativeFQN?.let { resolver.getClassDeclarationByName(it) }
    }

    private val targetType: KSType? by lazy { targetClassDeclaration?.asStarProjectedType() }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        if (targetType == null) return false

        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            mapType.isAssignableFrom(sourceNotNullable)
                && targetType!!.isAssignableFrom(targetNotNullable) && targetNotNullable.isExactlyTarget()
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

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
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

        val args = mutableListOf<Any>()

        var mapTypeChanged = false
        var changedTypes = false

        val mapSourceContentCode = when {
            genericSourceKeyType == genericTargetKeyType -> when {
                genericSourceValueType == genericTargetValueType -> fieldName
                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapValues·{·it.value!!·}"
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        changedTypes = true
                    }
                    fieldName
                }

                else -> {
                    mapTypeChanged = true
                    args += valueTypeConverter.convert(
                        "it",
                        genericSourceValueType,
                        genericTargetValueType
                    )
                    "$fieldName$nc.mapValues·{·(_,·it)·-> %L }"
                }
            }

            needsNotNullAssertionOperator(genericSourceKeyType, genericTargetKeyType) -> when {
                genericSourceValueType == genericTargetValueType -> {
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·it.key!!·}"
                }

                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    "$fieldName$nc.map·{·it.key!!·to·it.value!!·}$nc.toMap()"
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        changedTypes = true
                    }
                    mapTypeChanged = true
                    "$fieldName$nc.mapKeys·{·it.key!!·}"
                }

                else -> {
                    mapTypeChanged = true
                    args += valueTypeConverter.convert("value", genericSourceValueType, genericTargetValueType)
                    """
$fieldName$nc.map·{·(key,·value)·->
⇥val·newKey·=·key!!
val·newValue·=·%L
newKey·to·newValue
⇤}$nc.toMap()
                    """.trimIndent()
                }
            }

            genericSourceKeyType == genericTargetKeyType.makeNotNullable() -> {
                if (genericTargetKeyVariance == Variance.INVARIANT) {
                    changedTypes = true
                }
                when {
                    genericSourceValueType == genericTargetValueType -> fieldName
                    needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                        mapTypeChanged = true
                        "$fieldName$nc.mapValues·{·it.value!!·}"
                    }

                    genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                        if (genericTargetValueVariance == Variance.INVARIANT) {
                            changedTypes = true
                        }
                        fieldName
                    }

                    else -> {
                        mapTypeChanged = true
                        args += valueTypeConverter.convert(
                            "it",
                            genericSourceValueType,
                            genericTargetValueType
                        )
                        "$fieldName$nc.mapValues·{·(_,·it)·-> %L }"
                    }
                }
            }

            else -> when {
                genericSourceValueType == genericTargetValueType -> {
                    mapTypeChanged = true
                    args += keyTypeConverter.convert(
                        "it",
                        genericSourceKeyType,
                        genericTargetKeyType
                    )
                    "$fieldName$nc.mapKeys·{·(it,·_)·-> %L }"
                }

                needsNotNullAssertionOperator(genericSourceValueType, genericTargetValueType) -> {
                    mapTypeChanged = true
                    args += keyTypeConverter.convert("key", genericSourceKeyType, genericTargetKeyType)
                    """
$fieldName$nc.map·{·(key,·value)·->
⇥val·newKey·=·%L
val·newValue·=·value!!
newKey·to·newValue
⇤}$nc.toMap()
                    """.trimIndent()
                }

                genericSourceValueType == genericTargetValueType.makeNotNullable() -> {
                    if (genericTargetValueVariance == Variance.INVARIANT) {
                        changedTypes = true
                    }
                    mapTypeChanged = true
                    args += keyTypeConverter.convert("it", genericSourceKeyType, genericTargetKeyType)
                    "$fieldName$nc.mapKeys·{·(it,·_)·-> %L }"
                }

                else -> {
                    mapTypeChanged = true
                    args += keyTypeConverter.convert("key", genericSourceKeyType, genericTargetKeyType)
                    args += valueTypeConverter.convert("value", genericSourceValueType, genericTargetValueType)
                    """
$fieldName$nc.map·{·(key,·value)·->
⇥val·newKey·=·%L
val·newValue·=·%L
newKey·to·newValue
⇤}$nc.toMap()
                    """.trimIndent()
                }
            }
        }

        val mapSourceContainerCode = if (matchesTarget(mapTypeChanged, source)) {
            CodeBlock.of("")
        } else {
            this.convertMap(nc)
        }
        args += mapSourceContainerCode

        val code = mapSourceContentCode + "%L" + appendNotNullAssertionOperatorIfNeeded(source, target)

        return CodeBlock.of(
            if (changedTypes || castNeeded(genericSourceKeyType, genericTargetKeyType)) {
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

    open fun castNeeded(keySource: KSType, keyTarget: KSType): Boolean = false

    abstract fun convertMap(nc: String): CodeBlock

    open fun matchesTarget(mapTypeChanged: Boolean, source: KSType): Boolean {
        return !mapTypeChanged && source.isInstanceOfTarget()
    }

}

@AutoService(TypeConverter::class)
class MapToMapConverter : MapToXConverter(MAP) {
    override fun convertMap(nc: String): CodeBlock = CodeBlock.of("")
}

@AutoService(TypeConverter::class)
class MapToMutableMapConverter : MapToXConverter(MUTABLEMAP) {
    override fun convertMap(nc: String): CodeBlock = CodeBlock.of("$nc.toMutableMap()")
}

@AutoService(TypeConverter::class)
class MapToHashMapConverter : MapToXConverter(HASHMAP, JAVA_HASHMAP) {
    override fun convertMap(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMap(%T())", ClassName(KOTLIN_COLLECTIONS_PACKAGE, "HashMap"))
    }
}

@AutoService(TypeConverter::class)
class MapToLinkedHashMapConverter : MapToXConverter(LINKEDHASHMAP, JAVA_LINKEDHASHMAP) {
    override fun convertMap(nc: String): CodeBlock {
        return CodeBlock.of("$nc.toMap(%T())", ClassName(KOTLIN_COLLECTIONS_PACKAGE, "LinkedHashMap"))
    }
}

@AutoService(TypeConverter::class)
class MapToPersistentMapConverter : MapToXConverter(PERISTENT_MAP) {
    override fun castNeeded(keySource: KSType, keyTarget: KSType): Boolean {
        return !keySource.isNullable() && keyTarget.isNullable()
    }

    override fun convertMap(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName("kotlinx.collections.immutable", "toPersistentMap"))
    }
}

@AutoService(TypeConverter::class)
class MapToImmutableMapConverter : MapToXConverter(IMMUTABLE_MAP) {
    override fun castNeeded(keySource: KSType, keyTarget: KSType): Boolean {
        return !keySource.isNullable() && keyTarget.isNullable()
    }

    override fun convertMap(nc: String): CodeBlock {
        return CodeBlock.of("$nc.%M()", MemberName("kotlinx.collections.immutable", "toImmutableMap"))
    }
}
