package io.mcarle.konvert.processor

import io.mcarle.konvert.converter.api.TypeConverter

interface AnnotatedConverterData {
    fun toTypeConverters(): List<TypeConverter>
}
