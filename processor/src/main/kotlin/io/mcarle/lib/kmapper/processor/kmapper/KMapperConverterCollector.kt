package io.mcarle.lib.kmapper.processor.kmapper

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.lib.kmapper.api.annotation.KMapper
import io.mcarle.lib.kmapper.api.annotation.KMapping

object KMapperConverterCollector {

    @OptIn(KspExperimental::class)
    fun collect(resolver: Resolver): List<KMapperConverter> {
        return resolver.getSymbolsWithAnnotation(KMapper::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.INTERFACE) {
                    throw IllegalStateException("KMap can only target interfaces")
                }

                ksClassDeclaration
                    .getAllFunctions()
                    .filter { it.isAnnotationPresent(KMapping::class) }
                    .map {
                        val source =
                            if (it.parameters.size > 1) null else it.parameters.first().type.resolve().declaration as? KSClassDeclaration
                        val target = it.returnType?.resolve()?.declaration as? KSClassDeclaration

                        val kspMapping = it.getAnnotationsByType(KMapping::class).first()

                        if (source == null || target == null) {
                            throw IllegalStateException("KMapping annotated function must have exactly one parameter: $it")
                        }

                        KMapperConverter(
                            annotation = kspMapping,
                            sourceClassDeclaration = source,
                            targetClassDeclaration = target,
                            mapKSClassDeclaration = ksClassDeclaration,
                            mapKSFunctionDeclaration = it
                        )
                    }
            }.toList()
    }

}