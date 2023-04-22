package io.mcarle.konvert.injector.cdi

import jakarta.enterprise.context.RequestScoped

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KRequestScoped(
    val value: RequestScoped = RequestScoped()
)
