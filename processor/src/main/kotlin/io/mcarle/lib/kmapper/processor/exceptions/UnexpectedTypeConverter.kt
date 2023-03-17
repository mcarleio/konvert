package io.mcarle.lib.kmapper.processor.exceptions

import io.mcarle.lib.kmapper.converter.api.TypeConverter

class UnexpectedTypeConverter(typeConverter: TypeConverter) : RuntimeException(
    "Not expected a type converter of type ${typeConverter::class}"
)