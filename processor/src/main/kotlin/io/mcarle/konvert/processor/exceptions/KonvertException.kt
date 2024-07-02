package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSType

class KonvertException(
    source: KSType,
    target: KSType,
    cause: Exception
) : RuntimeException(
    "Error while processing $source -> $target",
    cause
)
