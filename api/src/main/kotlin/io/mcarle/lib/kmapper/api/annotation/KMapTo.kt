package io.mcarle.lib.kmapper.api.annotation

import io.mcarle.lib.kmapper.processor.api.DEFAULT_KMAPTO_PRIORITY
import io.mcarle.lib.kmapper.processor.api.Priority
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class KMapTo(
    val value: KClass<*>,
    val mappings: Array<KMap> = [],
    val mapFunctionName: String = "",
    val priority: Priority = DEFAULT_KMAPTO_PRIORITY
)
