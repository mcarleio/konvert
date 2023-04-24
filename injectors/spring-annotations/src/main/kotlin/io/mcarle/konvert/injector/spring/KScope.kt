package io.mcarle.konvert.injector.spring

import org.springframework.context.annotation.Scope

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KScope(
    val value: Scope = Scope(),
)
