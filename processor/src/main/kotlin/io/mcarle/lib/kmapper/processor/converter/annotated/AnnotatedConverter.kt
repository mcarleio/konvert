package io.mcarle.lib.kmapper.processor.converter.annotated

import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.Priority
import io.mcarle.lib.kmapper.processor.TypeConverter

sealed interface AnnotatedConverter<A : Annotation> : TypeConverter {
    val annotation: A
    val priority: Priority
}