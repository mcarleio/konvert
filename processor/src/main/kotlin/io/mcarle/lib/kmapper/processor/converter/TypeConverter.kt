package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.ConverterConfig

abstract class AbstractTypeConverter : TypeConverter {
    protected lateinit var config: ConverterConfig
    protected val resolver by lazy {
        config.resolver
    }

    override fun init(config: ConverterConfig) {
        this.config = config
    }

}

interface TypeConverter {
    fun init(config: ConverterConfig)
    fun matches(source: KSType, target: KSType): Boolean
    fun convert(fieldName: String, source: KSType, target: KSType): String
}