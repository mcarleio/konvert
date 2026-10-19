package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.converter.api.config.UNMAPPED_TARGET_POLICY_OPTION

class UnmappedTargetException(classDeclaration: KSClassDeclaration, targets: List<String>) : RuntimeException(
    "No source for the target parameter(s) $targets of ${(classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString()}: " +
        "map them, or exclude them with @Mapping(target = \"...\", ignore = true) " +
        "(${UNMAPPED_TARGET_POLICY_OPTION.key} is ERROR)"
)
