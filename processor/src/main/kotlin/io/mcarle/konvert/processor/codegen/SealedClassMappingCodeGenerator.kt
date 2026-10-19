package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.SealedClassMappingException

/**
 * Maps a sealed source onto a sealed target with an exhaustive `when` over the source's subclasses,
 * pairing each with the target subclass of the same simple name.
 */
class SealedClassMappingCodeGenerator(
    private val codeGenerator: CodeGenerator
) {

    private class Branch(val source: KSClassDeclaration, val code: CodeBlock)

    /**
     * A sealed source maps onto a sealed target subclass by subclass, or onto ONE non-sealed target when every source
     * subclass has a converter of its own to it.
     */
    fun isApplicable(context: MappingContext): Boolean {
        return context.sourceClassDeclaration.isSealed()
            && TypeConverterRegistry.none { it !is AnnotatedConverter && it.matches(context.source, context.target) }
            && (context.targetClassDeclaration.isSealed() || everySubclassHasOwnConverter(context))
    }

    private fun everySubclassHasOwnConverter(context: MappingContext): Boolean {
        val subclasses = context.sourceClassDeclaration.getSealedSubclasses().toList()
        return subclasses.isNotEmpty() && subclasses.all { ownConverter(it, context) != null }
    }

    /** A converter written for [subclass] itself, not one for the sealed parent, which would call itself. */
    private fun ownConverter(subclass: KSClassDeclaration, context: MappingContext) =
        TypeConverterRegistry.firstOrNull {
            it.matches(subclass.asStarProjectedType(), context.target)
                && !it.matches(context.sourceClassDeclaration.asStarProjectedType(), context.target)
        }

    fun generateCode(
        mappings: List<Mapping>,
        enforcedConstructorTypes: List<KSClassDeclaration>,
        context: MappingContext,
        visibilityContext: MappingVisibilityContext,
        additionalSourceParameters: List<KSValueParameter>
    ): CodeBlock {
        val source = context.sourceClassDeclaration
        val target = context.targetClassDeclaration
        if (source.typeParameters.isNotEmpty() || target.typeParameters.isNotEmpty()) {
            throw SealedClassMappingException.genericTypes(source, target)
        }
        val defaultConstructor = enforcedConstructorTypes.singleOrNull()?.qualifiedName?.asString() == Unit::class.qualifiedName
        if (mappings.isNotEmpty() || !defaultConstructor) {
            throw SealedClassMappingException.mappingsNotSupported(source, target)
        }
        if (context.source.isNullable() && !context.target.isNullable()) {
            throw SealedClassMappingException.nullableSourceToNotNullTarget(source, target)
        }

        val paramName = requireNotNull(context.paramName)
        val branches = if (target.isSealed()) {
            branches(source, target, context, enforcedConstructorTypes, visibilityContext, additionalSourceParameters)
        } else {
            source.getSealedSubclasses().map { subclass ->
                val subclassType = subclass.asStarProjectedType()
                Branch(subclass, requireNotNull(ownConverter(subclass, context)).convert(paramName, subclassType, context.target))
            }.toList()
        }.sortedBy { it.source.qualifiedName?.asString() }

        return CodeBlock.builder()
            .add("return·when·(%L)·{\n⇥", paramName)
            .apply { if (context.source.isNullable()) add("null·->·null\n") }
            .apply { branches.forEach { add("is·%T·->·%L\n", it.source.toClassName(), it.code) } }
            .add("⇤}")
            .build()
    }

    private fun branches(
        source: KSClassDeclaration,
        target: KSClassDeclaration,
        context: MappingContext,
        enforcedConstructorTypes: List<KSClassDeclaration>,
        visibilityContext: MappingVisibilityContext,
        additionalSourceParameters: List<KSValueParameter>
    ): List<Branch> {
        val targetSubclasses = target.getSealedSubclasses().toList()
        val pairs = source.getSealedSubclasses().associateWith { sourceSubclass ->
            val sameName = targetSubclasses.filter { it.simpleName.asString() == sourceSubclass.simpleName.asString() }
            val convertible = targetSubclasses.filter { targetSubclass ->
                TypeConverterRegistry.any { it.matches(sourceSubclass.asStarProjectedType(), targetSubclass.asStarProjectedType()) }
            }
            val candidates = when {
                convertible.size == 1 -> convertible
                convertible.size > 1 -> convertible.intersect(sameName.toSet()).ifEmpty { convertible }.toList()
                else -> sameName
            }
            if (candidates.size > 1) {
                throw SealedClassMappingException.ambiguousTargetSubclasses(sourceSubclass, target, candidates)
            }
            candidates.singleOrNull()
        }
        val unpaired = pairs.filterValues { it == null }.keys
        if (unpaired.isNotEmpty()) {
            throw SealedClassMappingException.missingTargetSubclasses(target, unpaired.toList())
        }

        return pairs.flatMap { (sourceSubclass, targetSubclass) ->
            requireNotNull(targetSubclass)
            val sourceType = sourceSubclass.asStarProjectedType()
            val targetType = targetSubclass.asStarProjectedType()
            val converter = TypeConverterRegistry.firstOrNull { it.matches(sourceType, targetType) }
            when {
                converter != null -> listOf(
                    Branch(sourceSubclass, converter.convert(context.paramName!!, sourceType, targetType))
                )

                targetSubclass.classKind == ClassKind.OBJECT -> listOf(
                    Branch(sourceSubclass, CodeBlock.of("%T", targetSubclass.toClassName()))
                )

                sourceSubclass.isSealed() && targetSubclass.isSealed() ->
                    branches(sourceSubclass, targetSubclass, context, enforcedConstructorTypes, visibilityContext, additionalSourceParameters)

                else -> listOf(
                    Branch(
                        sourceSubclass,
                        codeGenerator.generateCode(
                            mappings = emptyList(),
                            enforcedConstructorTypes = enforcedConstructorTypes,
                            context = MappingContext(
                                sourceClassDeclaration = sourceSubclass,
                                targetClassDeclaration = targetSubclass,
                                source = sourceType,
                                target = targetType,
                                paramName = context.paramName,
                                targetClassImportName = null,
                                asExpression = true,
                            ),
                            visibilityContext = visibilityContext,
                            additionalSourceParameters = additionalSourceParameters
                        )
                    )
                )
            }
        }
    }

    private fun KSClassDeclaration.isSealed() = Modifier.SEALED in modifiers
}
