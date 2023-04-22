package io.mcarle.konvert.injector.spring

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import io.mcarle.konvert.processor.api.KonverterInjector
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.lang.reflect.Proxy

@AutoService(KonverterInjector::class)
class SpringInjector : KonverterInjector {

    @OptIn(KspExperimental::class)
    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        originKSClassDeclaration.getAnnotationsByType(KScope::class).firstOrNull()?.also {
            builder.addAnnotation(buildScopeAnnotation(it.value))
        }
        originKSClassDeclaration.getAnnotationsByType(KComponent::class).firstOrNull()?.also {
            builder.addAnnotation(buildComponentAnnotation(it.value))
        }
    }

    @OptIn(DelicateKotlinPoetApi::class)
    private fun buildComponentAnnotation(component: Component): AnnotationSpec {
        return AnnotationSpec.get(component.extendProxy(), false)
    }

    @OptIn(DelicateKotlinPoetApi::class)
    private fun buildScopeAnnotation(scope: Scope): AnnotationSpec {
        return AnnotationSpec.get(scope.extendProxy(), false)
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
