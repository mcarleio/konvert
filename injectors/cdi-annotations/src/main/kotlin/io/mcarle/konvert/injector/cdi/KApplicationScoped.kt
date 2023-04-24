package io.mcarle.konvert.injector.cdi

import jakarta.enterprise.context.ApplicationScoped

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KApplicationScoped(
    val value: ApplicationScoped = ApplicationScoped()
)
