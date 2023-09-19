package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSType

class NoMatchingTypeConverterException(
    sourceName: String,
    sourceType: KSType,
    targetName: String,
    targetType: KSType
) : NoSuchElementException(
    "No converter for $sourceName -> $targetName: $sourceType -> $targetType"
)
