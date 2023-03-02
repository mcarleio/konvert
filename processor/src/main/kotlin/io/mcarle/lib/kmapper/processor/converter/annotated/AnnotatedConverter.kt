package io.mcarle.lib.kmapper.processor.converter.annotated

import io.mcarle.lib.kmapper.processor.api.TypeConverter

sealed interface AnnotatedConverter<A : Annotation> : TypeConverter {
    val annotation: A
}