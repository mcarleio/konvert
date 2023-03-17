package io.mcarle.lib.kmapper.api.annotation

import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPPER_PRIORITY
import io.mcarle.lib.kmapper.converter.api.Priority
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class KMapping(
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
     * The generated converter will get the defined priority.
     */
    val priority: Priority = DEFAULT_KMAPPER_PRIORITY
)