package io.mcarle.lib.kmapper.processor.api

import com.google.devtools.ksp.symbol.KSType

abstract class AbstractTypeConverter : TypeConverter {
    protected lateinit var config: ConverterConfig
    protected val resolver by lazy {
        config.resolver
    }

    override fun init(config: ConverterConfig) {
        this.config = config
    }

    fun sameType(
        source: KSType,
        target: KSType,
        sourceType: KSType,
        targetType: KSType
    ): Boolean {
        return when {
            source == sourceType && target == targetType -> true
            source == sourceType && target == targetType.makeNullable() -> true
            source == sourceType.makeNullable() && target == targetType.makeNullable() -> true

            source == sourceType.makeNullable() && target == targetType -> !config.enforceNotNull
            else -> false
        }
    }


    fun handleNullable(
        source: KSType,
        target: KSType,
        matchesWithBothTypesNotNull: (KSType, KSType) -> Boolean
    ): Boolean {
        if (needsNotNullAssertionOperator(source, target) && !config.enforceNotNull) return false
        return matchesWithBothTypesNotNull(source.makeNotNullable(), target.makeNotNullable())
    }

    fun needsNotNullAssertionOperator(
        source: KSType,
        target: KSType,
    ): Boolean {
        return source.isNullable() && !target.isNullable()
    }


    fun appendNotNullAssertionOperatorIfNeeded(source: KSType, target: KSType) = if (needsNotNullAssertionOperator(source, target)) {
        "!!"
    } else {
        ""
    }
}

interface TypeConverter {
    val enabledByDefault: Boolean
    val priority: Priority get() = DEFAULT_CONVERTER_PRIORITY
    fun init(config: ConverterConfig)
    fun matches(source: KSType, target: KSType): Boolean
    fun convert(fieldName: String, source: KSType, target: KSType): String
}