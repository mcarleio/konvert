package io.mcarle.lib.kmapper.converter.api

import com.google.devtools.ksp.processing.Resolver

data class ConverterConfig(
    val resolver: Resolver,
    val options: Options
)
