package io.mcarle.konvert.api

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GeneratedKonverter(
    val priority: Priority
)
