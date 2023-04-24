package io.mcarle.konvert.injector.spring

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.plugin.api.extendProxy
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

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

}
