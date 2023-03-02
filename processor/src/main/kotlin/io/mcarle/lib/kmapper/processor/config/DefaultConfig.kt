package io.mcarle.lib.kmapper.processor.config

import io.mcarle.lib.kmapper.processor.api.Config

object DefaultConfig : Config {
    override val enforceNotNull: Boolean = true
}