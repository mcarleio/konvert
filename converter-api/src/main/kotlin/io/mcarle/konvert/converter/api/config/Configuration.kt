package io.mcarle.konvert.converter.api.config

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import io.mcarle.konvert.api.Konfig

class Configuration internal constructor(
    internal val options: MutableMap<String, String>,
) : Map<String, String> by options {

    companion object {
        var CURRENT: Configuration = Configuration(mutableMapOf())
            internal set
    }

    override fun toString(): String {
        return options.toString()
    }

}

fun <T> withIsolatedConfiguration(environment: SymbolProcessorEnvironment, exec: () -> T): T {
    return withIsolatedConfiguration(environment.options, exec)
}

internal inline fun <T> withIsolatedConfiguration(options: Map<String, String>, exec: () -> T): T {
    val previous = Configuration.CURRENT
    Configuration.CURRENT = Configuration(options.toMutableMap())
    try {
        return exec()
    } finally {
        Configuration.CURRENT = previous
    }
}

fun <T> withIsolatedConfiguration(options: List<Konfig>, exec: () -> T): T {
    return withIsolatedConfiguration(Configuration.CURRENT.options + options.associate { it.key to it.value }, exec)
}
