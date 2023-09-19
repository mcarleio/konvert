package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.config.ENFORCE_NOT_NULL_OPTION

class NotNullOperatorNotEnabledException : RuntimeException {

    constructor(sourceName: String?, sourceType: KSType, targetType: KSType) : this(
        sourceName, sourceType, null, targetType
    )

    constructor(sourceName: String?, sourceType: KSType, targetName: String?, targetType: KSType) : this(
        source = if (sourceName.isNullOrEmpty()) "" else "$sourceName.$sourceType",
        target = if (targetName.isNullOrEmpty()) "" else "$targetName.$targetType"
    )

    private constructor(source: String, target: String) : super(
        """
            Mapping from $source to $target without !! operator not possible.

                Consider allowing such mappings with the option ${ENFORCE_NOT_NULL_OPTION.key}

                You can configure this globally or for a single converter:
                @KonvertTo(TargetClass::class, options = [
                    Konfig(key = "${ENFORCE_NOT_NULL_OPTION.key}", value = "true")
                ])

        """.trimIndent()
    )
}
