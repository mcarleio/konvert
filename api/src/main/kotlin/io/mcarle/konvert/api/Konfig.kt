package io.mcarle.konvert.api

@Retention(AnnotationRetention.SOURCE)
annotation class Konfig(
    val key: String,
    val value: String
) {
    companion object
}
