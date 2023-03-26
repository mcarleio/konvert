package io.mcarle.konvert.processor.exceptions

import io.mcarle.konvert.converter.api.TypeConverter

class UnexpectedTypeConverter(typeConverter: TypeConverter) : RuntimeException(
    "Not expected a type converter of type ${typeConverter::class}"
)
