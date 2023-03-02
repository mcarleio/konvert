package io.mcarle.lib.kmapper.processor.api

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability

fun KSType.isNullable(): Boolean {
    return this.isMarkedNullable || this.nullability == Nullability.NULLABLE || this.nullability == Nullability.PLATFORM
}