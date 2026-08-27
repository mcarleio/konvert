package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.processor.exceptions.AmbiguousConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingConstructorException
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy

object ConstructorResolver {
    fun determineConstructor(
        targetData: TargetDataExtractionStrategy.TargetData,
        sourceProperties: List<PropertyMappingInfo>,
        constructorTypes: List<KSClassDeclaration>
    ): TargetDataExtractionStrategy.TargetConstructor {
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
        constructors: List<TargetDataExtractionStrategy.TargetConstructor>,
        constructorTypes: List<KSClassDeclaration>
    ): TargetDataExtractionStrategy.TargetConstructor? {
        return constructors.firstOrNull { constructor ->
            constructor.parameters.mapNotNull { it.type.classDeclaration() } == constructorTypes
        }
    }

    private fun determineSingleOrEmptyConstructor(constructors: List<TargetDataExtractionStrategy.TargetConstructor>): TargetDataExtractionStrategy.TargetConstructor? {
        return if (constructors.size <= 1) {
            constructors.firstOrNull()
        } else {
            constructors.firstOrNull {
                it.parameters.isEmpty()
            }
        }
    }

    private fun findMatchingConstructors(
        constructors: List<TargetDataExtractionStrategy.TargetConstructor>,
        props: List<PropertyMappingInfo>
    ): List<TargetDataExtractionStrategy.TargetConstructor> {
        return constructors
            .filter {
                propertiesMatching(
                    props,
                    it.parameters
                )
            }
    }

    private fun propertiesMatching(
        props: List<PropertyMappingInfo>,
        parameters: List<TargetDataExtractionStrategy.TargetConstructorParameter>
    ): Boolean {
        if (props.size >= parameters.filter { !it.hasDefault }.size) {
            return parameters.all { parameter ->
                props.any { property ->
                    property.targetName == parameter.name && (!property.ignore || parameter.hasDefault)
                }
            }
        }
        return false
    }
}
