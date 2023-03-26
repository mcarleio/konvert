package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.processor.exceptions.AmbiguousConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingConstructorException
import io.mcarle.konvert.processor.typeClassDeclaration

class ConstructorResolver(
    private val logger: KSPLogger
) {
    fun determineConstructor(
        mappingCodeParentDeclaration: KSDeclaration,
        targetClassDeclaration: KSClassDeclaration,
        sourceProperties: List<PropertyMappingInfo>,
        constructorTypes: List<KSClassDeclaration>
    ): KSFunctionDeclaration {
        val visibleConstructors = targetClassDeclaration.getConstructors()
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }.toList()

        return if (constructorTypes.firstOrNull()?.qualifiedName?.asString() == Unit::class.qualifiedName) {
            if (targetClassDeclaration.primaryConstructor != null
                && targetClassDeclaration.primaryConstructor!!.isVisibleFrom(mappingCodeParentDeclaration)
                && propertiesMatching(
                    sourceProperties,
                    targetClassDeclaration.primaryConstructor!!.parameters
                )
            ) {
                // Primary constructor
                targetClassDeclaration.primaryConstructor!!
            } else {
                determineSingleOrEmptyConstructor(visibleConstructors)
                    ?: findMatchingConstructors(visibleConstructors, sourceProperties)
                        .let {
                            if (it.size > 1) {
                                throw AmbiguousConstructorException(targetClassDeclaration, it)
                            } else if (it.isEmpty()) {
                                throw NoMatchingConstructorException(targetClassDeclaration, *sourceProperties.toTypedArray())
                            } else {
                                it.first()
                            }
                        }
            }
        } else {
            findConstructorByParameterTypes(visibleConstructors, constructorTypes)
                ?: throw NoMatchingConstructorException(targetClassDeclaration, *constructorTypes.toTypedArray())
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
                    property.targetName == parameter.name?.asString() && !property.ignore
                }
            }
        }
        return false
    }
}
