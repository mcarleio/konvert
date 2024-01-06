package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.symbol.KSClassDeclaration

object CurrentInterfaceContext {
    var interfaceKSClassDeclaration: KSClassDeclaration? = null
}

inline fun <T> withCurrentKonverterInterface(interfaceKSClassDeclaration: KSClassDeclaration, code: () -> T): T {
    CurrentInterfaceContext.interfaceKSClassDeclaration = interfaceKSClassDeclaration
    try {
        return code()
    } finally {
        CurrentInterfaceContext.interfaceKSClassDeclaration = null
    }
}

