package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.config.enforceNotNullStrategy
import io.mcarle.konvert.converter.api.config.EnforceNotNullStrategy

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

    /**
     * Wraps the given [fieldName] according to the not-null enforcement configuration.
     *
     * - if no enforcement is needed: returns [fieldName] as is
     * - if enforcement is needed and strategy is [EnforceNotNullStrategy.ASSERTION_OPERATOR]:
     *   generates `expression!!`
     * - if enforcement is needed and strategy is [EnforceNotNullStrategy.REQUIRE_NOT_NULL]:
     *   generates `requireNotNull(expression) { "Value for '<expression>' must not be null" }`
     */
    protected fun applyNotNullEnforcementIfNeeded(
        fieldName: String,
        source: KSType,
        target: KSType
    ): CodeBlock {
        val needsNotNull = needsNotNullAssertionOperator(source, target)
        if (!needsNotNull) {
            return CodeBlock.of("%L", fieldName)
        }

        if (!Configuration.enforceNotNull) {
            return CodeBlock.of("%L", fieldName)
        }

        return when (Configuration.enforceNotNullStrategy) {
            EnforceNotNullStrategy.ASSERTION_OPERATOR ->
                CodeBlock.of("%L!!", fieldName)

            EnforceNotNullStrategy.REQUIRE_NOT_NULL ->
                CodeBlock.of(
                    "requireNotNull(%L) { \"Value for '%L' must not be null\" }",
                    fieldName,
                    fieldName
                )
        }
    }


    @Deprecated("Use applyNotNullEnforcementIfNeeded instead", ReplaceWith("applyNotNullEnforcementIfNeeded(fieldName, source, target)"))
    fun appendNotNullAssertionOperatorIfNeeded(source: KSType, target: KSType) =
        if (needsNotNullAssertionOperator(source, target)) "!!" else ""
}
