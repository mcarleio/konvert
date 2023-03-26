package io.mcarle.kmap.api.annotation

import io.mcarle.kmap.converter.api.DEFAULT_KMAPTO_PRIORITY
import io.mcarle.kmap.converter.api.Priority
import kotlin.reflect.KClass

/**
 * Annotate a class to generate an extension function to map this instance to the target class
 *
 * Example:
 * ```kotlin
 * @KMapTo(Target::class, mappings=[KMap(source="source", target="target")])
 * class Source(val source: Int)
 * class Target(val target: String)
 * ```
 *
 * This will generate an extension function in the same package as the annotated class:
 * ```kotlin
 * fun Source.mapToTarget() = Target(target = this.source.toString())
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class KMapTo(
    /**
     * The target class of the mapping.
     */
    val value: KClass<*>,
    /**
     * List of user defined mappings for non-default use-cases.
     * During code generation all properties from source are appended (like `KMap(source=sourcePropertyName, target=sourcePropertyName)`)
     */
    val mappings: Array<KMap> = [],
    /**
     * Define the parameter types of a specific constructor of the target class which should be used.
     */
    val constructor: Array<KClass<*>> = [Unit::class],
    /**
     * If not set, defaults to `mapTo${value.simpleName}`
     */
    val mapFunctionName: String = "",
    /**
     * The generated converter will get the defined priority.
     */
    val priority: Priority = DEFAULT_KMAPTO_PRIORITY
)
