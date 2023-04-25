package io.mcarle.konvert.injector.koin

import org.koin.core.annotation.Factory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KFactory(
    val value: Factory = Factory(),
)
