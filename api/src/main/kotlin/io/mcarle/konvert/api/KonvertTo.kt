package io.mcarle.konvert.api

import io.mcarle.konvert.converter.api.DEFAULT_KONVERT_TO_PRIORITY
import io.mcarle.konvert.converter.api.Priority
import kotlin.reflect.KClass

/**
 * Annotate a class to generate an extension function to map this instance to the target class
 *
 * Example:
 * ```kotlin
 * @KonvertTo(Target::class, mappings=[Mapping(source="source", target="target")])
 * class Source(val source: Int)
 * class Target(val target: String)
 * ```
 *
 * This will generate an extension function in the same package as the annotated class:
 * ```kotlin
 * fun Source.toTarget() = Target(target = this.source.toString())
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class KonvertTo(
    /**
     * The target class of the mapping.
     */
    val value: KClass<*>,
    /**
     * List of user defined mappings for non-default use-cases.
     * During code generation all properties from source are appended (like `Mapping(source=sourcePropertyName, target=sourcePropertyName)`)
     */
    val mappings: Array<Mapping> = [],
    /**
     * Define the parameter types of a specific constructor of the target class which should be used.
     */
    val constructor: Array<KClass<*>> = [Unit::class],
    /**
     * If not set, defaults to `to${value.simpleName}`
     */
    val mapFunctionName: String = "",
    /**
     * The generated converter will get the defined priority.
     */
    val priority: Priority = DEFAULT_KONVERT_TO_PRIORITY,

    val options: Array<Konfig> = []
)
