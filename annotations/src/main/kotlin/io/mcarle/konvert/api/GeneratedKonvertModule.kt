package io.mcarle.konvert.api

/**
 * Annotation that is used by Konvert to define a module containing FQNs to generated mapping functions of Konvert
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class GeneratedKonvertModule(
    val konverterFQN: Array<String>,
    val konvertToFQN: Array<String>,
    val konvertFromFQN: Array<String>
)
