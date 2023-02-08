package io.mcarle.lib.kmapper.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class KMapping(
    val mappings: Array<KMap> = []
)