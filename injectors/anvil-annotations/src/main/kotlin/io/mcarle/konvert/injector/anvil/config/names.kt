package io.mcarle.konvert.injector.anvil.config

/**
 * Append some injection method by default to all generated mapper classes. Requires `konvert.anvil.default-scope` to be set as well!
 *
 * Possible values: `factory` and `singleton`
 *
 * Default: disabled
 */
const val DEFAULT_INJECTION_METHOD = "konvert.anvil.default-injection-method"

/**
 * Has to be set to qualified name of anvil scope class, like AppScope
 *
 * Default: ""
 */
const val DEFAULT_SCOPE = "konvert.anvil.default-scope"
