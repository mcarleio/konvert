package io.mcarle.konvert.injector.koin.config

/**
 * Append some injection method by default to all generated mapper classes.
 *
 * Possible values: `factory`, `single`, `scope` (requires `konvert.koin.default-scope` to be set as well!)
 *
 * Default: disabled
 */
const val DEFAULT_INJECTION_METHOD = "konvert.koin.default-injection-method"

/**
 * Use this scope by default when `konvert.koin.default-injection-method` is set to `scope`.
 *
 * - If value is fully qualified class identifier it will by used as `@Scope(ProvidedType::class)`.
 * - If value is string - it will be used as named scope, like `@Scope(name = "ProvidedName")`
 *
 * Default: ""
 */
const val DEFAULT_SCOPE = "konvert.koin.default-scope"
