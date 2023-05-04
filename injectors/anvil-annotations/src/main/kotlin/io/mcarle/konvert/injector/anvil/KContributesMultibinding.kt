package io.mcarle.konvert.injector.anvil

import com.squareup.anvil.annotations.ContributesMultibinding

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KContributesMultibinding(
    val value: ContributesMultibinding,
)
