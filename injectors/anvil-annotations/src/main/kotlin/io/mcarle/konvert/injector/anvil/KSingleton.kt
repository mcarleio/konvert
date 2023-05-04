package io.mcarle.konvert.injector.anvil

import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KSingleton(
    val value: Singleton = Singleton(),
)
