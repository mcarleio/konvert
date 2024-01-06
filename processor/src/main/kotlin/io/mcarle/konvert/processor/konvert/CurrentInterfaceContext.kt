package io.mcarle.konvert.processor.konvert

object CurrentInterfaceContext {
    var konverterInterface: KonverterInterface? = null
}

inline fun <T> withCurrentKonverterInterface(konverterInterface: KonverterInterface, code: () -> T): T {
    CurrentInterfaceContext.konverterInterface = konverterInterface
    try {
        return code()
    } finally {
        CurrentInterfaceContext.konverterInterface = null
    }
}

