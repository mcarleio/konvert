package io.mcarle.konvert.injector.koin

import org.koin.core.annotation.Single

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KSingle(
    val value: Single = Single(),
)
