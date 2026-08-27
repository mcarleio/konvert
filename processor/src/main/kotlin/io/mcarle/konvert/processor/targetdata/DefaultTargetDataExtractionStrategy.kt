package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext
import io.mcarle.konvert.processor.codegen.isVisibleFrom
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.TypeSubstitution

class DefaultTargetDataExtractionStrategy : TargetDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        targetType: ResolvedType,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext
    ): TargetDataExtractionStrategy.TargetData {
        val primaryConstructor = classDeclaration.primaryConstructor.takeIf { it?.isVisibleFrom(visibilityContext) == true }

        val substitution by lazy { TypeSubstitution.of(targetType, resolver) }

        val properties = classDeclaration.getAllProperties()
            .filter { it.extensionReceiver == null }
            .filter { it.isVisibleFrom(visibilityContext) }
            .filter { it.isMutable }
            .map { TargetDataExtractionStrategy.TargetVarProperty(it, substitution, resolver) }

        val setters = classDeclaration.getAllFunctions()
            .filter { it.extensionReceiver == null }
            .filter { it.origin in listOf(Origin.JAVA, Origin.JAVA_LIB) }
            .filter { it.parameters.size == 1 }
            .filter { isSetter(it.simpleName.asString()) }
            .filter { it.isVisibleFrom(visibilityContext) }
            .map {
                TargetDataExtractionStrategy.TargetSetter(
                    it,
                    determineCorrespondingGetter(it, classDeclaration, resolver),
                    substitution,
                    resolver
                )
            }

        return TargetDataExtractionStrategy.TargetData(
            classDeclaration = classDeclaration,
            varProperties = properties.toList(),
            setter = setters.toList(),
            primaryConstructor = primaryConstructor?.let {
                TargetDataExtractionStrategy.TargetConstructor(it, substitution, resolver)
            },
            constructors = classDeclaration.getConstructors()
                .filter { it.isVisibleFrom(visibilityContext) }
                .map { TargetDataExtractionStrategy.TargetConstructor(it, substitution, resolver) }
                .toList()
        )
    }

    private fun isSetter(functionName: String) = functionName.startsWith("set") && !functionName[3].isLowerCase()

    private fun determineCorrespondingGetter(
        setter: KSFunctionDeclaration,
        classDeclaration: KSClassDeclaration,
        resolver: Resolver
    ): KSFunctionDeclaration? {
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
        val propertyNamePascalCase =
            TargetDataExtractionStrategy.TargetSetter.extractPropertyName(setter).replaceFirstChar { it.uppercase() }
        return if (setter.parameters.first().type.resolve() == booleanType) {
            arrayOf("is$propertyNamePascalCase", "get$propertyNamePascalCase")
        } else {
            arrayOf("get$propertyNamePascalCase")
        }
    }
}
