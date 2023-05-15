@file:Suppress("ClassName")

package io.mcarle.konvert.injector.anvil.config

import io.mcarle.konvert.converter.api.config.Option

/**
 * Append some injection method by default to all generated mapper classes. Requires `konvert.anvil.default-scope` to be set as well!
 *
 * Possible values: `factory` and `singleton`
 *
 * Default: disabled
 */
object DEFAULT_INJECTION_METHOD_OPTION : Option<InjectionMethod>("konvert.anvil.default-injection-method", InjectionMethod.DISABLED)

/**
 * Has to be set to qualified name of anvil scope class, like AppScope
 *
 * Default: ""
 */
object DEFAULT_SCOPE_OPTION : Option<String>("konvert.anvil.default-scope", "")

enum class InjectionMethod {
    DISABLED,
    FACTORY,
    SINGLETON
}
