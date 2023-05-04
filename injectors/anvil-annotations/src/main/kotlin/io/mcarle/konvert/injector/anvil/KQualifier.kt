package io.mcarle.konvert.injector.anvil

import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KQualifier(
    val value: Qualifier,
)
