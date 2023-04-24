package io.mcarle.konvert.plugin.api

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.TypeSpec

/**
 * Allows to inject custom logic into generated types from a @[io.mcarle.konvert.api.Konverter] annotated interface
 */
interface KonverterInjector {

    fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration)

}
