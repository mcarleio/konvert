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
        expression: CodeBlock,
        fieldName: String?,
        source: KSType,
        target: KSType
    ): CodeBlock {
        val needsNotNull = needsNotNullAssertionOperator(source, target)
        if (!needsNotNull) {
            return expression
        }

        if (!Configuration.enforceNotNull) {
            throw IllegalStateException(
                "Not-null enforcement is required to map from nullable '$source' to non-nullable '$target', " +
                    "but '${io.mcarle.konvert.converter.api.config.ENFORCE_NOT_NULL_OPTION.key}' is set to false."
            )
        }

        return when (Configuration.enforceNotNullStrategy) {
            EnforceNotNullStrategy.ASSERTION_OPERATOR ->
                CodeBlock.of("%L!!", expression)

            EnforceNotNullStrategy.REQUIRE_NOT_NULL -> {
                val message = fieldName
                    ?.let { "Value for '$it' must not be null" }
                    ?: "Value must not be null"
                CodeBlock.of("requireNotNull(%L) { %S }", expression, message)
            }
        }
    }


    @Deprecated("Use applyNotNullEnforcementIfNeeded instead", ReplaceWith("applyNotNullEnforcementIfNeeded(fieldName, source, target)"))
    fun appendNotNullAssertionOperatorIfNeeded(source: KSType, target: KSType) =
        if (needsNotNullAssertionOperator(source, target)) "!!" else ""
}
