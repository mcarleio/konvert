package io.mcarle.kmap.processor.exceptions

import io.mcarle.kmap.converter.api.TypeConverter

class UnexpectedTypeConverter(typeConverter: TypeConverter) : RuntimeException(
    "Not expected a type converter of type ${typeConverter::class}"
)