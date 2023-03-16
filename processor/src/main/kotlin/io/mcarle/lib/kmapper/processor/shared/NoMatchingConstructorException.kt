package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.symbol.KSClassDeclaration

class NoMatchingConstructorException(classDeclaration: KSClassDeclaration, availableProperties: List<String>) :
    RuntimeException("No constructor for $classDeclaration matching the available properties $availableProperties found") {
    constructor(classDeclaration: KSClassDeclaration, vararg availableProperties: KSClassDeclaration) : this(
        classDeclaration,
        availableProperties.map { it.simpleName.asString() }
    )

    constructor(classDeclaration: KSClassDeclaration, vararg availableProperties: PropertyMappingInfo) : this(
        classDeclaration,
        availableProperties.map { it.targetName }
    )
}
