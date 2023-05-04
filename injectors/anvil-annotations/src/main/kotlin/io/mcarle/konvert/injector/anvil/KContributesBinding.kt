package io.mcarle.konvert.injector.anvil

import com.squareup.anvil.annotations.ContributesBinding

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KContributesBinding(
    val value: ContributesBinding,
)
