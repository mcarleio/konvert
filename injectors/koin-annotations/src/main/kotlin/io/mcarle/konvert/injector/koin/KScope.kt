package io.mcarle.konvert.injector.koin

import org.koin.core.annotation.Scope

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KScope(
    val value: Scope
)
