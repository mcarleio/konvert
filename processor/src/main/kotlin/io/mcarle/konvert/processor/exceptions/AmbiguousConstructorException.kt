package io.mcarle.konvert.processor.exceptions

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class AmbiguousConstructorException(classDeclaration: KSClassDeclaration, constructors: List<KSFunctionDeclaration>) :
    RuntimeException("Ambiguous constructors for $classDeclaration: ${constructors.map { c -> c.parameters.map { p -> p.type } }}")
