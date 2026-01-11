package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.konverterUseReflection
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter

class KonvertTypeConverter constructor(
    override val priority: Priority,
    override val alreadyGenerated: Boolean,
    /**
     * indicates if the converter was (alreadyGenerated = true) or will be generated as a class or object
     */
    internal val classKind: ClassOrObject,
    internal val sourceType: KSType,
    internal val targetType: KSType,
    internal val mapFunctionName: String,
    internal val paramName: String,
    internal val konverterInterface: KonverterInterface
) : AbstractTypeConverter(), AnnotatedConverter {

    enum class ClassOrObject {
        CLASS,
        OBJECT;
    }

    override val enabledByDefault: Boolean = true

    private val targetTypeNotNullable: KSType = targetType.makeNotNullable()

    // different and more complex handling than others, as the converter's types here can be nullable themselves
    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, _ ->
            if (!sourceType.isAssignableFrom(sourceNotNullable)) {
                // cannot pass the current source (ignoring its nullability) to the converter
                return@handleNullable false
            }

            if (!target.isAssignableFrom(targetTypeNotNullable)) {
                // cannot assign the output of the converter (ignoring its nullability) to the required target type
                return@handleNullable false
            }

            true
        }
    }

    override fun needsNotNullAssertionOperator(source: KSType, target: KSType): Boolean {
        if (source.isNullable() && !sourceType.isNullable() && !target.isNullable()) {
            // as the converter expects a not null source value but the actual source type might be null
            // the result of this converter will also be nullable. If the target is not nullable, then
            // a not null assertion operator is needed
            return true
        }

        if (targetType.isNullable() && !target.isNullable()) {
            // the output of the converter is nullable, but the target is not nullable, therefore
            // it would need a not null assertion operator
            return true
        }

        return false
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val params = mutableListOf<Any>()
        val getKonverterCode = if (CurrentInterfaceContext.konverterInterface == konverterInterface) {
            CodeBlock.of("this")
        } else {
            if (Configuration.konverterUseReflection) {
                CodeBlock.of(
                    format = "%T.get<%T>()",
                    Konverter::class,
                    konverterInterface.typeName
                )
            } else {
                CodeBlock.of(
                    format = if (classKind == ClassOrObject.CLASS) "%T()" else "%T",
                    ClassName(
                        konverterInterface.packageName,
                        konverterInterface.simpleName + Konverter.KONVERTER_GENERATED_CLASS_SUFFIX
                    )
                )
            }
        }
        params += getKonverterCode
        val mappingCode = if (source.isNullable() && !sourceType.isNullable()) {
            "$fieldName?.let·{ %L.$mapFunctionName($paramName·=·it) }"
        } else {
            "%L.$mapFunctionName($paramName·=·$fieldName)"
        }

        val expression = CodeBlock.of(mappingCode, *params.toTypedArray())

        return applyNotNullEnforcementIfNeeded(
            expression = expression,
            fieldName = fieldName,
            source = source,
            target = target
        )
    }

}
