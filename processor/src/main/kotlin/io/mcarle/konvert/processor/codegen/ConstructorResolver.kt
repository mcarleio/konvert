package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.processor.exceptions.AmbiguousConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingConstructorException
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy
import io.mcarle.konvert.processor.typeClassDeclaration

object ConstructorResolver {
    fun determineConstructor(
        targetData: TargetDataExtractionStrategy.TargetData,
        sourceProperties: List<PropertyMappingInfo>,
        constructorTypes: List<KSClassDeclaration>
    ): KSFunctionDeclaration {
        return if (constructorTypes.firstOrNull()?.qualifiedName?.asString() == Unit::class.qualifiedName) {
            if (targetData.primaryConstructor != null
                && propertiesMatching(
                    sourceProperties,
                    targetData.primaryConstructor.parameters
                )
            ) {
                targetData.primaryConstructor
            } else {
                determineSingleOrEmptyConstructor(targetData.constructors)
                    ?: findMatchingConstructors(targetData.constructors, sourceProperties)
                        .let {
                            if (it.size > 1) {
                                throw AmbiguousConstructorException(targetData.classDeclaration, it)
                            } else if (it.isEmpty()) {
                                throw NoMatchingConstructorException(targetData.classDeclaration, *sourceProperties.toTypedArray())
                            } else {
                                it.first()
                            }
                        }
            }
        } else {
            findConstructorByParameterTypes(targetData.constructors, constructorTypes)
                ?: throw NoMatchingConstructorException(targetData.classDeclaration, *constructorTypes.toTypedArray())
        }
    }

    private fun findConstructorByParameterTypes(
        constructors: List<KSFunctionDeclaration>,
        constructorTypes: List<KSClassDeclaration>
    ): KSFunctionDeclaration? {
        return constructors.firstOrNull { constructor ->
            constructor.parameters.mapNotNull { it.typeClassDeclaration() } == constructorTypes
        }
    }

    private fun determineSingleOrEmptyConstructor(constructors: List<KSFunctionDeclaration>): KSFunctionDeclaration? {
        return if (constructors.size <= 1) {
            constructors.firstOrNull()
        } else {
            constructors.firstOrNull {
                it.parameters.isEmpty()
            }
        }
    }

    private fun findMatchingConstructors(
        constructors: List<KSFunctionDeclaration>,
        props: List<PropertyMappingInfo>
    ): List<KSFunctionDeclaration> {
        return constructors
            .filter {
                propertiesMatching(
                    props,
                    it.parameters
                )
            }
    }

    private fun propertiesMatching(props: List<PropertyMappingInfo>, parameters: List<KSValueParameter>): Boolean {
        if (props.size >= parameters.filter { !it.hasDefault }.size) {
            return parameters.all { parameter ->
                props.any { property ->
//                    property.targetName == parameter.name?.asString() && !property.ignore
                    property.targetName == parameter.name?.asString() && (!property.ignore || parameter.hasDefault)
                }
            }
        }
        return false
    }
}
