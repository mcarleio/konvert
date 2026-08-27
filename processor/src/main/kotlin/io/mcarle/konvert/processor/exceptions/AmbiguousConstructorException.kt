package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy

class AmbiguousConstructorException(classDeclaration: KSClassDeclaration, constructors: List<TargetDataExtractionStrategy.TargetConstructor>) :
    RuntimeException("Ambiguous constructors for $classDeclaration: ${constructors.map { c -> c.parameters.map { p -> p.type } }.joinToString(", ")}")
