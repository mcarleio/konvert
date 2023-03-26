package io.mcarle.kmap.converter.api

import com.google.devtools.ksp.processing.Resolver

data class ConverterConfig(
    val resolver: Resolver,
    val options: Options
)
