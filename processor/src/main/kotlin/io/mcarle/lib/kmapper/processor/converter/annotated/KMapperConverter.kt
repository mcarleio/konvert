package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.api.KMappers
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.processor.api.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.api.Priority

class KMapperConverter constructor(
    override val annotation: KMapping,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapKSFunctionDeclaration: KSFunctionDeclaration,
) : AbstractTypeConverter(), AnnotatedConverter<KMapping> {

    override val priority: Priority = annotation.priority
    val mapFunctionName: String = mapKSFunctionDeclaration.simpleName.asString()
    val paramName: String = mapKSFunctionDeclaration.parameters.first().name!!.asString()

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType == source && targetType == target
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return "${KMappers::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>().$mapFunctionName($paramName = $fieldName)"
    }

    override val enabledByDefault: Boolean = true
}