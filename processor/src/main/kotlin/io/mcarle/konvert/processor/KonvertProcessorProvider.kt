package io.mcarle.konvert.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class KonvertProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val processor = KonvertProcessor(environment)
        environment.registerProcessorForUpcomingFeaturesIfSupported(processor)
        return processor
    }

    /**
     * Older KSP versions do not provide `SymbolProcessorEnvironment.registerProcessorForNewFeatures` at all.
     * Calling it reflectively keeps the processor compatible with those versions, while still opting in to
     * the upcoming features when a newer KSP version is used.
     */
    private fun SymbolProcessorEnvironment.registerProcessorForUpcomingFeaturesIfSupported(processor: SymbolProcessor) {
        val method = runCatching {
            javaClass.getMethod("getRegisterProcessorForNewFeatures")
        }.getOrNull() ?: return

        @Suppress("UNCHECKED_CAST")
        val register = runCatching {
            method.invoke(this) as (SymbolProcessor) -> Unit
        }.getOrNull() ?: return

        register(processor)
    }
}
