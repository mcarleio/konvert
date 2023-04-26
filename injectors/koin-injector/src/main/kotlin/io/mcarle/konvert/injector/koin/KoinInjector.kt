package io.mcarle.konvert.injector.koin

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.plugin.api.extendProxy
import com.squareup.kotlinpoet.ksp.toTypeName
import org.koin.core.annotation.Scope

@AutoService(KonverterInjector::class)
class KoinInjector : KonverterInjector {

    @OptIn(KspExperimental::class, DelicateKotlinPoetApi::class)
    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        originKSClassDeclaration.getAnnotationsByType(KSingle::class).firstOrNull()?.also {
            builder.addAnnotation(AnnotationSpec.get(it.value.extendProxy(), false))
        }
        originKSClassDeclaration.getAnnotationsByType(KFactory::class).firstOrNull()?.also {
            builder.addAnnotation(AnnotationSpec.get(it.value.extendProxy(), false))
        }
        originKSClassDeclaration.getAnnotationsByType(KNamed::class).firstOrNull()?.also {
            builder.addAnnotation(AnnotationSpec.get(it.value.extendProxy(), false))
        }

        val scopeParseResult = Result.runCatching {
            originKSClassDeclaration.getAnnotationsByType(KScope::class).firstOrNull()?.also {
                builder.addAnnotation(AnnotationSpec.get(it.value.extendProxy(), false))
            }
        }
        if (scopeParseResult.isFailure) {
            originKSClassDeclaration.annotations
                .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KScope::class.qualifiedName }
                .firstOrNull()
                ?.also {
                    it.arguments.firstOrNull()?.also { argument ->
                        val scope = argument.value as KSAnnotation
                        builder.addAnnotation(scope.toAnnotationSpec())
                    }
                }
        }

        originKSClassDeclaration.getAnnotationsByType(KScoped::class).firstOrNull()?.also {
            builder.addAnnotation(AnnotationSpec.get(it.value.extendProxy(), false))
        }
    }
}
