package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSType

class NotNullOperatorNotEnabledException(source: KSType, target: KSType) : RuntimeException(
    "Can not generate mapping from $source to $target without !! operator"
)
