package io.mcarle.konvert.injector.cdi

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.plugin.api.extendProxy

@AutoService(KonverterInjector::class)
class CdiInjector : KonverterInjector {

    @OptIn(KspExperimental::class, DelicateKotlinPoetApi::class)
    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        originKSClassDeclaration.getAnnotationsByType(KApplicationScoped::class).firstOrNull()?.also {
            builder.addAnnotation(
                AnnotationSpec.get(it.value.extendProxy(), false)
            )
        }
        originKSClassDeclaration.getAnnotationsByType(KSessionScoped::class).firstOrNull()?.also {
            builder.addAnnotation(
                AnnotationSpec.get(it.value.extendProxy(), false)
            )
        }
        originKSClassDeclaration.getAnnotationsByType(KRequestScoped::class).firstOrNull()?.also {
            builder.addAnnotation(
                AnnotationSpec.get(it.value.extendProxy(), false)
            )
        }
    }
}
