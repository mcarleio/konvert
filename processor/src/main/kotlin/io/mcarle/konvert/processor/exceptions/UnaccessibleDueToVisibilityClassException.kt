package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Visibility

class UnaccessibleDueToVisibilityClassException(
    visibility: Visibility,
    classDeclaration: KSClassDeclaration
) : RuntimeException(
    "The class ${(classDeclaration.qualifiedName ?: classDeclaration.simpleName).asString()} is not accessible due to its $visibility visibility"
)
