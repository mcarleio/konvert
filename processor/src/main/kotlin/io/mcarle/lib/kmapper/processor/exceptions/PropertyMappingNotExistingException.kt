package io.mcarle.lib.kmapper.processor.exceptions

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.lib.kmapper.processor.codegen.CodeGenerator
import io.mcarle.lib.kmapper.processor.codegen.PropertyMappingInfo

class PropertyMappingNotExistingException(target: String, propertyMappings: List<PropertyMappingInfo>) : RuntimeException(
    "No property for $target existing in $propertyMappings"
) {
    constructor(ksValueParameter: KSValueParameter, propertyMappings: List<PropertyMappingInfo>): this(
        ksValueParameter.toString(),
        propertyMappings
    )
    constructor(ksPropertyDeclaration: KSPropertyDeclaration, propertyMappings: List<PropertyMappingInfo>): this(
        ksPropertyDeclaration.toString(),
        propertyMappings
    )
    constructor(targetElement: CodeGenerator.TargetElement, propertyMappings: List<PropertyMappingInfo>): this(
        targetElement.toString(),
        propertyMappings
    )
}
