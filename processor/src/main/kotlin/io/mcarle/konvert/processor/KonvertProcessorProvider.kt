package io.mcarle.konvert.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class KonvertProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return io.mcarle.konvert.processor.KonvertProcessor(
            codeGenerator = environment.codeGenerator,
            options = io.mcarle.konvert.converter.api.Options(environment.options),
            logger = environment.logger
        )
    }
}
