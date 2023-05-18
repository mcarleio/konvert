package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.symbol.KSClassDeclaration

object CurrentInterfaceContext {
    var interfaceKSClassDeclaration: KSClassDeclaration? = null
}
