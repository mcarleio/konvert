package io.mcarle.lib.kmapper.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.lib.kmapper.processor.codegen.PropertyMappingInfo

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
