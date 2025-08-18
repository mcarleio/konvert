package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.converter.api.config.invalidMappingStrategy
import io.mcarle.konvert.processor.exceptions.InvalidMappingException
import io.mcarle.konvert.processor.sourcedata.SourceDataExtractionStrategy

class PropertyMappingResolver(
    private val logger: KSPLogger,
) {
    fun determinePropertyMappings(
        mappingParamName: String?,
        mappings: List<Mapping>,
        additionalSourceParameters: List<KSValueParameter>,
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>
    ): List<PropertyMappingInfo> {
        val propertiesWithoutSource = getPropertyMappingsWithoutSource(mappings, mappingParamName)
        val propertiesWithSource = getPropertyMappingsWithSource(mappings, sourceDataList, mappingParamName)
        val propertiesFromAdditionalParameters = getPropertyMappingsFromAdditionalParameters(additionalSourceParameters)
        val propertiesWithoutMappings = getPropertyMappingsWithoutMappings(sourceDataList, mappingParamName)

        return propertiesWithoutSource + propertiesWithSource + propertiesFromAdditionalParameters + propertiesWithoutMappings
    }

    private fun getPropertyMappingsFromAdditionalParameters(
        properties: List<KSValueParameter>,
    ) = properties
        .map { property ->
            val paramName = property.name!!.asString()
            PropertyMappingInfo(
                mappingParamName = null,
                sourceName = null,
                targetName = paramName,
                constant = paramName,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                sourceData = null,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithoutMappings(
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        mappingParamName: String?
    ) = sourceDataList
        .map { sourceData ->
            PropertyMappingInfo(
                mappingParamName = mappingParamName,
                sourceName = sourceData.name,
                targetName = sourceData.name,
                constant = null,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                sourceData = sourceData,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithSource(
        mappings: List<Mapping>,
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        mappingParamName: String?
    ): List<PropertyMappingInfo> {

        val missingSourceMappings = mappings
            .filter { it.source.isNotEmpty() }
            .filter { sourceDataList.none { property -> property.name == it.source } }

        if (missingSourceMappings.isNotEmpty()) {
            when (Configuration.invalidMappingStrategy) {
                InvalidMappingStrategy.FAIL -> throw InvalidMappingException.missingSource(missingSourceMappings)
                InvalidMappingStrategy.WARN -> {
                    missingSourceMappings.forEach {
                        logger.warn("Ignoring the mapping $it as the source field '${it.source}' does not exist.")
                    }
                }
            }
        }

        return (mappings - missingSourceMappings)
            .filter { it.source.isNotEmpty() }
            .map { annotation ->
                annotation to sourceDataList.first { property ->
                    property.name == annotation.source
                }
            }
            .map { (annotation, sourceData) ->
                PropertyMappingInfo(
                    mappingParamName = mappingParamName,
                    sourceName = sourceData.name,
                    targetName = annotation.target,
                    constant = annotation.constant.takeIf { it.isNotEmpty() },
                    expression = annotation.expression.takeIf { it.isNotEmpty() },
                    ignore = annotation.ignore,
                    enableConverters = annotation.enable.toList(),
                    sourceData = sourceData,
                    isBasedOnAnnotation = true
                )
            }

    }

    private fun getPropertyMappingsWithoutSource(
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
            enableConverters = annotation.enable.toList(),
            sourceData = null,
            isBasedOnAnnotation = true
        )
    }

}
