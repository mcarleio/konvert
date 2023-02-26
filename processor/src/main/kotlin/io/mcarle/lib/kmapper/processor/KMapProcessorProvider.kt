package io.mcarle.lib.kmapper.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import io.mcarle.lib.kmapper.processor.config.CliOptions
import io.mcarle.lib.kmapper.processor.converter.annotated.*

@AutoService(SymbolProcessorProvider::class)
class KMapProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KMapProcessor(
            codeGenerator = environment.codeGenerator,
            cliOptions = CliOptions.from(environment.options),
            logger = environment.logger
        )
    }
}