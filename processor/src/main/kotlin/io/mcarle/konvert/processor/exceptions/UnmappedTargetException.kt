package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.converter.api.config.UNMAPPED_TARGET_POLICY_OPTION

class UnmappedTargetException(
    classDeclaration: KSClassDeclaration,
    parameters: List<String>,
    properties: List<String> = emptyList()
) : RuntimeException(
    "No source for the " +
        listOfNotNull(
            parameters.takeIf { it.isNotEmpty() }?.let { "target parameter(s) $it" },
            properties.takeIf { it.isNotEmpty() }?.let { "target property(ies) $it" }
        ).joinToString(" and the ") +
        " of ${(classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString()}: " +
        "map them, or exclude them with @Mapping(target = \"...\", ignore = true) " +
        "(${UNMAPPED_TARGET_POLICY_OPTION.key} is ERROR)"
)
