package io.mcarle.konvert.injector.koin

import org.koin.core.annotation.Named

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KNamed(
    val value: Named
)
