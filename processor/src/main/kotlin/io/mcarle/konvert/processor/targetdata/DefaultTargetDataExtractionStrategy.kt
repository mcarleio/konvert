package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin

class DefaultTargetDataExtractionStrategy : TargetDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration
    ): TargetDataExtractionStrategy.TargetData {
        val primaryConstructor = if (classDeclaration.primaryConstructor?.isVisibleFrom(mappingCodeParentDeclaration) == true) {
            classDeclaration.primaryConstructor
        } else {
            null
        }

        val properties = classDeclaration.getAllProperties()
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .filter { it.extensionReceiver == null }
            .mapNotNull {
                if (it.isMutable) {
                    TargetDataExtractionStrategy.TargetVarProperty(it)
                } else {
                    null
                }
            }

        val setters = classDeclaration.getAllFunctions()
            .filter { it.extensionReceiver == null }
            .filter { it.origin in listOf(Origin.JAVA, Origin.JAVA_LIB) }
            .filter { it.parameters.size == 1 }
            .filter { it.simpleName.asString().startsWith("set") }
            .filter { it.returnType?.resolve() == resolver.builtIns.unitType }
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .map {
                TargetDataExtractionStrategy.TargetSetter(
                    it,
                    determineCorrespondingGetter(it, classDeclaration, resolver)
                )
            }

        return TargetDataExtractionStrategy.TargetData(
            classDeclaration = classDeclaration,
            varProperties = properties.toList(),
            setter = setters.toList(),
            primaryConstructor = primaryConstructor,
            constructors = classDeclaration.getConstructors()
                .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
                .toList()
        )
    }

    private fun determineCorrespondingGetter(setter: KSFunctionDeclaration, classDeclaration: KSClassDeclaration, resolver: Resolver): KSFunctionDeclaration? {
        val booleanType = resolver.builtIns.booleanType

        return classDeclaration.getAllFunctions()
            .filter { it.extensionReceiver == null }
            .filter { it.origin in listOf(Origin.JAVA, Origin.JAVA_LIB) }
            .filter { it.parameters.isEmpty() }
            .filter { it.returnType?.resolve() == setter.parameters.first().type.resolve() }
            .filter { it.simpleName.asString() in allowedGetterNames(setter, booleanType) }
            .singleOrNull()
    }

    private fun allowedGetterNames(setter: KSFunctionDeclaration, booleanType: KSType): Array<String> {
        val propertyNamePascalCase = TargetDataExtractionStrategy.TargetSetter.extractPropertyName(setter).replaceFirstChar { it.uppercase() }
        return if (setter.parameters.first().type.resolve() == booleanType) {
            arrayOf("is$propertyNamePascalCase", "get$propertyNamePascalCase")
        } else {
            arrayOf("get$propertyNamePascalCase")
        }
    }
}
