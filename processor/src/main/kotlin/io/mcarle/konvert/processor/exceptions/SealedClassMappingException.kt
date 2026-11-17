package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration

class SealedClassMappingException(message: String) : RuntimeException(message) {

    companion object {
        fun missingTargetSubclasses(target: KSClassDeclaration, sources: List<KSClassDeclaration>) =
            SealedClassMappingException(
                "No subclass of ${target.name()} has the simple name of, or a converter from, ${sources.map { it.name() }.sorted()}"
            )

        fun ambiguousTargetSubclasses(source: KSClassDeclaration, target: KSClassDeclaration, candidates: List<KSClassDeclaration>) =
            SealedClassMappingException(
                "${source.name()} matches more than one subclass of ${target.name()}: ${candidates.map { it.name() }.sorted()}"
            )

        fun genericTypes(source: KSClassDeclaration, target: KSClassDeclaration) =
            SealedClassMappingException("Mapping generic sealed types is not supported: ${source.name()} -> ${target.name()}")

        fun mappingsNotSupported(source: KSClassDeclaration, target: KSClassDeclaration) =
            SealedClassMappingException(
                "Mappings and constructor selection are not supported between sealed types (${source.name()} -> ${target.name()}), declare a function per subclass pair instead"
            )

        fun nullableSourceToNotNullTarget(source: KSClassDeclaration, target: KSClassDeclaration) =
            SealedClassMappingException(
                "A nullable sealed source needs a nullable target: ${source.name()}? -> ${target.name()}"
            )

        private fun KSClassDeclaration.name() = (qualifiedName ?: simpleName).asString()
    }
}
