package io.mcarle.konvert.injector.spring

import org.springframework.stereotype.Component

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KComponent(
    val value: Component = Component(),
)
