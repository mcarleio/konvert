package io.mcarle.konvert.api

import io.mcarle.konvert.converter.api.DEFAULT_KONVERT_PRIORITY
import io.mcarle.konvert.converter.api.Priority
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Konvert(
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
     * The generated converter will get the defined priority.
     */
    val priority: Priority = DEFAULT_KONVERT_PRIORITY,

    val options: Array<Konfig> = []
)
