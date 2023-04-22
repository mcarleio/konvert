package io.mcarle.konvert.injector.cdi

import jakarta.enterprise.context.SessionScoped

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KSessionScoped(
    val value: SessionScoped = SessionScoped()
)
