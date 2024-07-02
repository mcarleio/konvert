package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.codegen.PropertyMappingInfo

class PropertyMappingNotExistingException(target: String, propertyMappings: List<PropertyMappingInfo>) : RuntimeException(
    "No property for `$target` existing. Available mappings are: ${propertyMappings.map { it.targetName }}"
) {
    constructor(ksValueParameter: KSValueParameter, propertyMappings: List<PropertyMappingInfo>) : this(
        ksValueParameter.toString(),
        propertyMappings
    )

    constructor(ksPropertyDeclaration: KSPropertyDeclaration, propertyMappings: List<PropertyMappingInfo>) : this(
        ksPropertyDeclaration.toString(),
        propertyMappings
    )

    constructor(targetElement: CodeGenerator.TargetElement, propertyMappings: List<PropertyMappingInfo>) : this(
        targetElement.toString(),
        propertyMappings
    )
}
