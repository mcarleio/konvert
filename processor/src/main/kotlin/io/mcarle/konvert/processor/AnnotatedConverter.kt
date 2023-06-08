package io.mcarle.konvert.processor

import io.mcarle.konvert.converter.api.TypeConverter

fun interface AnnotatedConverterData {
    fun toTypeConverters(): List<AnnotatedConverter>
}

interface AnnotatedConverter: TypeConverter {
    val alreadyGenerated: Boolean
}
