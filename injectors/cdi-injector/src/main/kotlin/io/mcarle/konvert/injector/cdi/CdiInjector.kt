package io.mcarle.konvert.injector.cdi

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import io.mcarle.konvert.processor.api.KonverterInjector
import java.lang.reflect.Proxy

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

    private inline fun <reified T : Any> T.extendProxy(): T {
        return if (Proxy.isProxyClass(this::class.java)) {
            val ih = Proxy.getInvocationHandler(this)
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
                if (method.name == "annotationType") {
                    T::class.java
                } else {
                    ih.invoke(proxy, method, args)
                }
            } as T
        } else {
            this
        }
    }
}
