@file:Suppress("ClassName")

package io.mcarle.konvert.injector.koin.config

import io.mcarle.konvert.converter.api.config.Option

/**
 * Append some injection method by default to all generated mapper classes.
 *
 * Possible values: `factory`, `single`, `scope` (requires `konvert.koin.default-scope` to be set as well!)
 *
 * Default: disabled
 */
object DEFAULT_INJECTION_METHOD_OPTION : Option<InjectionMethod>("konvert.koin.default-injection-method", InjectionMethod.DISABLED)

/**
 * Use this scope by default when `konvert.koin.default-injection-method` is set to `scope`.
 *
 * - If value is fully qualified class identifier it will by used as `@Scope(ProvidedType::class)`.
 * - If value is string - it will be used as named scope, like `@Scope(name = "ProvidedName")`
 *
 * Default: ""
 */
object DEFAULT_SCOPE_OPTION : Option<String>("konvert.koin.default-scope", "")

enum class InjectionMethod {
    DISABLED,
    FACTORY,
    SINGLE,
    SCOPE
}
