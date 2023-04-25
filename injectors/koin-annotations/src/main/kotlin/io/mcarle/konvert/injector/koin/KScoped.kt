package io.mcarle.konvert.injector.koin

import org.koin.core.annotation.Scoped

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KScoped(
    val value: Scoped = Scoped()
)
