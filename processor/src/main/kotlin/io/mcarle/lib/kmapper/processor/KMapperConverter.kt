package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.annotation.KMappers
import io.mcarle.lib.kmapper.processor.converter.AbstractTypeConverter

class KMapperConverter(
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapFunctionName: String,
    val paramName: String,
) : AbstractTypeConverter() {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType == source && targetType == target
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return "${KMappers::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>().$mapFunctionName($paramName = $fieldName)"
    }
}