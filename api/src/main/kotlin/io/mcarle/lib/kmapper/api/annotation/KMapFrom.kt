package io.mcarle.lib.kmapper.api.annotation

import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPFROM_PRIORITY
import io.mcarle.lib.kmapper.converter.api.Priority
import kotlin.reflect.KClass

/**
 * Annotate a class (it must have a companion object) or directly its companion object to generate
 * an extension function to generate an instance of the class from the provided source.
 *
 * Example:
 * ```kotlin
 * class Source(val source: Int)
 * class Target(val target: String) {
 *   @KMapFrom(Source::class, mappings=[KMap(source="source", target="target")])
 *   companion object
 * }
 * ```
 *
 * This will generate an extension function in the same package as the annotated class:
 * ```kotlin
 * fun Target.Companion.fromSource(source: Source) = Target(target = source.source.toString())
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class KMapFrom(
    /**
     * The source class of the mapping
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
     * If not set, defaults to `from${value.simpleName}`
     */
    val mapFunctionName: String = "",
    /**
     * The generated converter will get the defined priority.
     */
    val priority: Priority = DEFAULT_KMAPFROM_PRIORITY
)
