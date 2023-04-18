package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

class PropertyMappingResolver(
    private val logger: KSPLogger
) {
    fun determinePropertyMappings(
        mappingParamName: String?,
        mappings: List<Mapping>,
        type: KSType
    ): List<PropertyMappingInfo> {
        val classDeclaration = type.classDeclaration()!!
        val properties = classDeclaration.getAllProperties().toList()

        verifyAllPropertiesExist(mappings, properties, classDeclaration)

        val propertiesWithoutSource = getPropertyMappingsWithoutSource(type.isNullable(), mappings, mappingParamName)
        val propertiesWithSource = getPropertyMappingsWithSource(type.isNullable(), mappings, properties, mappingParamName)
        val propertiesWithoutMappings = getPropertyMappingsWithoutMappings(type.isNullable(), properties, mappingParamName)

        return propertiesWithoutSource + propertiesWithSource + propertiesWithoutMappings
    }

    private fun getPropertyMappingsWithoutMappings(
        nullable: Boolean,
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
                nullable = nullable,
                enableConverters = emptyList(),
                declaration = property,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithSource(
        nullable: Boolean,
        mappings: List<Mapping>,
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
            nullable = nullable,
            enableConverters = annotation.enable.toList(),
            declaration = property,
            isBasedOnAnnotation = true
        )
    }

    private fun getPropertyMappingsWithoutSource(
        nullable: Boolean,
        mappings: List<Mapping>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isEmpty() }.map { annotation ->
        PropertyMappingInfo(
            mappingParamName = mappingParamName,
            sourceName = null,
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            nullable = nullable,
            enableConverters = annotation.enable.toList(),
            declaration = null,
            isBasedOnAnnotation = true
        )
    }

    private fun verifyAllPropertiesExist(
        mappings: List<Mapping>,
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
