package io.mcarle.lib.kmapper.api.annotation

import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPPER_PRIORITY
import io.mcarle.lib.kmapper.converter.api.Priority

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class KMapping(
    val mappings: Array<KMap> = [],
    val priority: Priority = DEFAULT_KMAPPER_PRIORITY
)