package io.mcarle.lib.kmapper.annotation

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
