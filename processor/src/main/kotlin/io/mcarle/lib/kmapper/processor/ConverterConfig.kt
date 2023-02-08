package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.processing.Resolver

data class ConverterConfig(
    val resolver: Resolver,
    val enforceNotNull: Boolean = false
)
