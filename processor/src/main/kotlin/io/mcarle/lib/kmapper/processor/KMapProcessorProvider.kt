package io.mcarle.lib.kmapper.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import io.mcarle.lib.kmapper.converter.api.Options

@AutoService(SymbolProcessorProvider::class)
class KMapProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KMapProcessor(
            codeGenerator = environment.codeGenerator,
            options = Options(environment.options),
            logger = environment.logger
        )
    }
}