package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.processing.Resolver

data class ConverterConfig(
    val resolver: Resolver,
    val options: io.mcarle.konvert.converter.api.Options
)
