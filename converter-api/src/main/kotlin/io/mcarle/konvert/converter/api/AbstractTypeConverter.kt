package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enforceNotNull

abstract class AbstractTypeConverter(name: String? = null) : TypeConverter {
    override val name: String = name ?: this::class.java.simpleName
    protected lateinit var resolver: Resolver

    override fun init(resolver: Resolver) {
        this.resolver = resolver
    }

    fun handleNullable(
        source: KSType,
        target: KSType,
        matchesWithBothTypesNotNull: (KSType, KSType) -> Boolean
    ): Boolean {
        if (needsNotNullAssertionOperator(source, target) && !Configuration.enforceNotNull) return false
        return matchesWithBothTypesNotNull(source.makeNotNullable(), target.makeNotNullable())
    }

    open fun needsNotNullAssertionOperator(
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
