package io.mcarle.konvert.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.converter.config.enforceNotNull

abstract class AbstractTypeConverter : TypeConverter {
    protected lateinit var config: io.mcarle.konvert.converter.api.ConverterConfig
    protected val resolver by lazy {
        config.resolver
    }

    override fun init(config: io.mcarle.konvert.converter.api.ConverterConfig) {
        this.config = config
    }

    fun handleNullable(
        source: KSType,
        target: KSType,
        matchesWithBothTypesNotNull: (KSType, KSType) -> Boolean
    ): Boolean {
        if (needsNotNullAssertionOperator(source, target) && !config.options.enforceNotNull) return false
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
