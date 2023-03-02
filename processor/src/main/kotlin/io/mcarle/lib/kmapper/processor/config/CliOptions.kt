package io.mcarle.lib.kmapper.processor.config

import com.google.devtools.ksp.processing.Resolver
import io.mcarle.lib.kmapper.processor.api.Config
import io.mcarle.lib.kmapper.processor.api.ConverterConfig

data class CliOptions(
    override val enforceNotNull: Boolean?
) : Config {
    companion object {
        fun from(options: Map<String, String>) = CliOptions(
            enforceNotNull = options[CliOptions::enforceNotNull.name]?.toBoolean()
        )
    }

    fun toConverterConfig(resolver: Resolver) = ConverterConfig(
        resolver,
        enforceNotNull ?: DefaultConfig.enforceNotNull
    )
}