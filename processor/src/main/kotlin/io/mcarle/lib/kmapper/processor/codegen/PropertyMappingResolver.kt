package io.mcarle.lib.kmapper.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.mcarle.lib.kmapper.api.annotation.KMap

class PropertyMappingResolver(
    private val logger: KSPLogger
) {
    fun determinePropertyMappings(
        mappingParamName: String?,
        mappings: List<KMap>,
        classDeclaration: KSClassDeclaration
    ): List<PropertyMappingInfo> {
        val properties = classDeclaration.getAllProperties().toList()

        verifyAllPropertiesExist(mappings, properties, classDeclaration)

        val propertiesWithoutSource = getPropertyMappingsWithoutSource(mappings, mappingParamName)
        val propertiesWithSource = getPropertyMappingsWithSource(mappings, properties, mappingParamName)
        val propertiesWithoutMappings = getPropertyMappingsWithoutMappings(properties, mappingParamName)

        return propertiesWithoutSource + propertiesWithSource + propertiesWithoutMappings
    }

    private fun getPropertyMappingsWithoutMappings(
        properties: List<KSPropertyDeclaration>,
        mappingParamName: String?
    ) = properties
        .map { property ->
            PropertyMappingInfo(
                mappingParamName = mappingParamName,
                sourceName = property.simpleName.asString(),
                targetName = property.simpleName.asString(),
                constant = null,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                declaration = property,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithSource(
        mappings: List<KMap>,
        properties: List<KSPropertyDeclaration>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isNotEmpty() }.mapNotNull { annotation ->
        properties.firstOrNull { property ->
            property.simpleName.asString() == annotation.source
        }?.let { annotation to it }
    }.map { (annotation, property) ->
        PropertyMappingInfo(
            mappingParamName = mappingParamName,
            sourceName = property.simpleName.asString(),
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            declaration = property,
            isBasedOnAnnotation = true
        )
    }

    private fun getPropertyMappingsWithoutSource(
        mappings: List<KMap>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isEmpty() }.map { annotation ->
        PropertyMappingInfo(
            mappingParamName = mappingParamName,
            sourceName = null,
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            declaration = null,
            isBasedOnAnnotation = true
        )
    }

    private fun verifyAllPropertiesExist(
        mappings: List<KMap>,
        properties: List<KSPropertyDeclaration>,
        ksClassDeclaration: KSClassDeclaration
    ) {
        mappings.map { it.source }.filter { it.isNotEmpty() }.forEach { source ->
            if (properties.none { it.simpleName.asString() == source }) {
                logger.warn("Ignoring mapping: $source not existing in ${ksClassDeclaration.simpleName.asString()}")
            }
        }
    }
}