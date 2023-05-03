package io.mcarle.konvert.injector.koin.config

import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.get

val Configuration.Companion.defaultInjectionMethod: InjectionMethod
    get() = KoinOptions.DEFAULT_INJECTION_METHOD_OPTION.get(CURRENT) {
        InjectionMethod.valueOf(it.uppercase())
    }

val Configuration.Companion.defaultScope: String
    get() = KoinOptions.DEFAULT_SCOPE_OPTION.get(CURRENT) { it }
