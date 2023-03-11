package io.mcarle.lib.kmapper.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.isNullable

@AutoService(TypeConverter::class)
class EnumToEnumConverter : AbstractTypeConverter() {

    private val enumType: KSType by lazy {
        resolver.getClassDeclarationByName<Enum<*>>()!!.asStarProjectedType()
    }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            enumType.isAssignableFrom(sourceNotNullable) && enumType.isAssignableFrom(targetNotNullable)
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceEnumValues =
            (source.declaration as? KSClassDeclaration)?.declarations?.filterIsInstance<KSClassDeclaration>()?.toList() ?: emptyList()
        val targetEnumValues =
            (target.declaration as? KSClassDeclaration)?.declarations?.filterIsInstance<KSClassDeclaration>()?.toList() ?: emptyList()

        val missingEnumValue = sourceEnumValues.firstOrNull { sourceEnumValue ->
            targetEnumValues.none { it.simpleName.asString() == sourceEnumValue.simpleName.asString() }
        }

        require(missingEnumValue == null) {
            "Missing enum value in $target for $source.$missingEnumValue"
        }


        // @formatter:off
        return """
when·($fieldName)·{${ "\n⇥" +
        sourceEnumValues.joinToString("\n") {
            "${source.declaration.simpleName.asString()}.${it.simpleName.asString()}·->·${target.declaration.simpleName.asString()}.${it.simpleName.asString()}"
        }.let {
            if (source.isNullable()) {
                "$it\nnull·->·null"
            } else {
                it
            }
        }
}
⇤}${if (needsNotNullAssertionOperator(source, target)) "!!" else ""}
        """.trimIndent()
        // @formatter:on
    }

}