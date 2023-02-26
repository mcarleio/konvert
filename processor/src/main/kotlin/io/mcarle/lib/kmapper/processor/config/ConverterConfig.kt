package io.mcarle.lib.kmapper.processor.config

import com.google.devtools.ksp.processing.Resolver

data class ConverterConfig(
    val resolver: Resolver,
    override val enforceNotNull: Boolean,
) : Config
