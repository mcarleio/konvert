package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.converter.api.config.invalidMappingStrategy
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.exceptions.AmbiguousSourceException
import io.mcarle.konvert.processor.exceptions.InvalidMappingException
import io.mcarle.konvert.processor.sourcedata.SourceDataExtractionStrategy

class PropertyMappingResolver(
    private val logger: KSPLogger,
) {
    /**
     * The mappings of a function with a single source. A [Mapping.source] with a dot is a path through nested properties,
     * resolved like a path of several sources (`identity.sessionId`, or qualified by the parameter: `cart.identity.sessionId`).
     */
    fun determinePropertyMappings(
        mappingParamName: String?,
        mappings: List<Mapping>,
        additionalSourceParameters: List<KSValueParameter>,
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        sourceType: KSType,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): List<PropertyMappingInfo> {
        // a nullable single source is mapped inside `source?.let { ... }`, so a path starts from the smart cast, non-null value
        val pathSource = SourceWithData(mappingParamName, sourceType.makeNotNullable(), sourceDataList)
        val propertiesWithoutSource = getPropertyMappingsWithoutSource(mappings, mappingParamName)
        val propertiesWithSource = getPropertyMappingsWithSource(mappings, sourceDataList, mappingParamName, pathSource, extractSourceData)
        val propertiesFromAdditionalParameters = getPropertyMappingsFromAdditionalParameters(additionalSourceParameters)
        val propertiesWithoutMappings = getPropertyMappingsWithoutMappings(sourceDataList, mappingParamName)

        return propertiesWithoutSource + propertiesWithSource + propertiesFromAdditionalParameters + propertiesWithoutMappings
    }

    /**
     * The mappings of a function with several source parameters. A [Mapping.source] names a property of one source, either
     * qualified by the parameter (`enrichment.status`, also nested: `enrichment.customer.name`) or unqualified when
     * exactly one source has it. Same named properties of different sources are all returned, the caller rejects the
     * targets they would make ambiguous.
     */
    fun determinePropertyMappings(
        sources: List<SourceWithData>,
        mappings: List<Mapping>,
        additionalSourceParameters: List<KSValueParameter>,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): List<PropertyMappingInfo> {
        val propertiesWithoutSource = mappings.filter { it.source.isEmpty() }.map { annotation ->
            PropertyMappingInfo(
                mappingParamName = null,
                sourceName = null,
                targetName = annotation.target,
                constant = annotation.constant.takeIf { it.isNotEmpty() },
                expression = annotation.expression.takeIf { it.isNotEmpty() },
                ignore = annotation.ignore,
                enableConverters = annotation.enable.toList(),
                sourceData = null,
                isBasedOnAnnotation = true,
                expressionWithoutReceiver = true
            )
        }
        val propertiesWithSource = getPropertyMappingsWithSourcePath(mappings, sources, extractSourceData)
        val propertiesFromAdditionalParameters = getPropertyMappingsFromAdditionalParameters(additionalSourceParameters)
        val propertiesWithoutMappings = sources.flatMap { source ->
            source.sourceDataList.map { sourceData ->
                val step = SourcePathStep.start(source).next(sourceData)
                PropertyMappingInfo(
                    mappingParamName = source.paramName,
                    sourceName = sourceData.name,
                    targetName = sourceData.name,
                    constant = null,
                    expression = null,
                    ignore = false,
                    enableConverters = emptyList(),
                    sourceData = sourceData,
                    isBasedOnAnnotation = false,
                    sourcePath = step.toSourcePath()
                )
            }
        }

        return propertiesWithoutSource + propertiesWithSource + propertiesFromAdditionalParameters + propertiesWithoutMappings
    }

    private fun getPropertyMappingsWithSourcePath(
        mappings: List<Mapping>,
        sources: List<SourceWithData>,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): List<PropertyMappingInfo> {
        val resolvedMappings = mappings
            .filter { it.source.isNotEmpty() }
            .associateWith { resolveSourcePath(it, sources, extractSourceData) }

        handleMissingSources(resolvedMappings.filterValues { it == null }.keys)

        return resolvedMappings.mapNotNull { (annotation, path) ->
            if (path == null) return@mapNotNull null
            PropertyMappingInfo(
                mappingParamName = path.source.paramName,
                sourceName = path.sourceData.name,
                targetName = annotation.target,
                constant = annotation.constant.takeIf { it.isNotEmpty() },
                expression = annotation.expression.takeIf { it.isNotEmpty() },
                ignore = annotation.ignore,
                enableConverters = annotation.enable.toList(),
                sourceData = path.sourceData,
                isBasedOnAnnotation = true,
                sourcePath = path.toSourcePath(),
                expressionWithoutReceiver = true
            )
        }
    }

    /**
     * @return `null` if the path does not exist
     */
    private fun resolveSourcePath(
        mapping: Mapping,
        sources: List<SourceWithData>,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): SourcePathStep? {
        val segments = mapping.source.split('.')
        val qualifyingSource = sources.firstOrNull { it.paramName == segments.first() }
        if (qualifyingSource != null && segments.size > 1) {
            return walk(qualifyingSource, segments.drop(1), extractSourceData)
        }

        val candidates = sources.mapNotNull { walk(it, segments, extractSourceData) }
        if (candidates.size > 1) {
            throw AmbiguousSourceException(mapping.target, candidates.map { "${it.source.paramName}.${mapping.source}" })
        }
        return candidates.firstOrNull()
    }

    private fun walk(
        source: SourceWithData,
        segments: List<String>,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): SourcePathStep? {
        var step = SourcePathStep.start(source)
        var sourceDataList = source.sourceDataList
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                val classDeclaration = step.sourceData.typeRef.resolve().classDeclaration() ?: return null
                sourceDataList = extractSourceData(classDeclaration)
            }
            step = step.next(sourceDataList.firstOrNull { it.name == segment } ?: return null)
        }
        return step
    }

    /**
     * A source parameter with the properties readable from it
     */
    data class SourceWithData(
        /**
         * `null` when the source is the receiver (`@KonvertTo`)
         */
        val paramName: String?,
        val type: KSType,
        val sourceDataList: List<SourceDataExtractionStrategy.SourceData>
    )

    private class SourcePathStep private constructor(
        val source: SourceWithData,
        private val step: SourceDataExtractionStrategy.SourceData?,
        private val code: String,
        private val nullable: Boolean,
        private val type: KSType,
    ) {
        val sourceData: SourceDataExtractionStrategy.SourceData get() = step!!

        fun next(sourceData: SourceDataExtractionStrategy.SourceData): SourcePathStep {
            val type = sourceData.typeRef.resolve()
            return SourcePathStep(
                source = source,
                step = sourceData,
                code = when {
                    code.isEmpty() -> sourceData.accessCode
                    nullable -> "$code?.${sourceData.accessCode}"
                    else -> "$code.${sourceData.accessCode}"
                },
                nullable = nullable || type.isNullable(),
                type = type
            )
        }

        fun toSourcePath() = SourcePath(
            code = code,
            type = if (nullable) type.makeNullable() else type
        )

        companion object {
            fun start(source: SourceWithData) = SourcePathStep(
                source = source,
                step = null,
                code = source.paramName ?: "",
                nullable = source.type.isNullable(),
                type = source.type
            )
        }
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
        mappingParamName: String?,
        pathSource: SourceWithData,
        extractSourceData: (KSClassDeclaration) -> List<SourceDataExtractionStrategy.SourceData>
    ): List<PropertyMappingInfo> {
        val (pathMappings, propertyMappings) = mappings
            .filter { it.source.isNotEmpty() }
            .partition { '.' in it.source }

        val resolvedPaths = pathMappings.associateWith { annotation ->
            resolveSourcePath(annotation, listOf(pathSource), extractSourceData)
        }
        val resolvedProperties = propertyMappings.associateWith { annotation ->
            sourceDataList.firstOrNull { it.name == annotation.source }
        }

        val missing = resolvedPaths.filterValues { it == null }.keys + resolvedProperties.filterValues { it == null }.keys
        handleMissingSources(mappings.filter { it in missing })

        return mappings.mapNotNull { annotation ->
            val path = resolvedPaths[annotation]
            val sourceData = path?.sourceData ?: resolvedProperties[annotation] ?: return@mapNotNull null
            PropertyMappingInfo(
                mappingParamName = mappingParamName,
                sourceName = sourceData.name,
                targetName = annotation.target,
                constant = annotation.constant.takeIf { it.isNotEmpty() },
                expression = annotation.expression.takeIf { it.isNotEmpty() },
                ignore = annotation.ignore,
                enableConverters = annotation.enable.toList(),
                sourceData = sourceData,
                isBasedOnAnnotation = true,
                sourcePath = path?.toSourcePath()
            )
        }
    }

    private fun handleMissingSources(missingSourceMappings: Collection<Mapping>) {
        if (missingSourceMappings.isEmpty()) return
        when (Configuration.invalidMappingStrategy) {
            InvalidMappingStrategy.FAIL -> throw InvalidMappingException.missingSource(missingSourceMappings)
            InvalidMappingStrategy.WARN -> {
                missingSourceMappings.forEach {
                    logger.warn("Ignoring the mapping $it as the source field '${it.source}' does not exist.")
                }
            }
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
