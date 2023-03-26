package io.mcarle.kmap.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.kmap.converter.api.ConverterConfig
import io.mcarle.kmap.converter.api.TypeConverter
import io.mcarle.kmap.converter.api.isNullable
import io.mcarle.kmap.converter.config.enforceNotNull

abstract class AbstractTypeConverter : TypeConverter {
    protected lateinit var config: ConverterConfig
    protected val resolver by lazy {
        config.resolver
    }

    override fun init(config: ConverterConfig) {
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