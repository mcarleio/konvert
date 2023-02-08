package io.mcarle.lib.kmapper.annotation

@Retention(AnnotationRetention.SOURCE)
annotation class KMap(
    val source: String = "",
    val target: String,
    val constant: String = "",
    val expression: String = "",
    val ignore: Boolean = false
)