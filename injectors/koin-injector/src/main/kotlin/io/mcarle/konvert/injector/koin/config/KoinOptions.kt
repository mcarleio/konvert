package io.mcarle.konvert.injector.koin.config

import io.mcarle.konvert.converter.api.config.Option

object KoinOptions {
    val DEFAULT_INJECTION_METHOD_OPTION = Option(DEFAULT_INJECTION_METHOD, InjectionMethod.DISABLED)
    val DEFAULT_SCOPE_OPTION = Option(DEFAULT_SCOPE, "")
}

enum class InjectionMethod {
    DISABLED,
    FACTORY,
    SINGLE,
    SCOPE
}
