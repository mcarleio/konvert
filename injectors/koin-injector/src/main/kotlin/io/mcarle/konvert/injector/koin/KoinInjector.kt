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
import org.koin.core.annotation.*

@AutoService(KonverterInjector::class)
class KoinInjector : KonverterInjector {

    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        passthroughAnnotation<KSingle, Single>(builder, originKSClassDeclaration) {
            it.value
        }
        passthroughAnnotation<KFactory, Factory>(builder, originKSClassDeclaration) {
            it.value
        }
        passthroughAnnotation<KNamed, Named>(builder, originKSClassDeclaration) {
            it.value
        }
        passthroughAnnotation<KScope, Scope>(builder, originKSClassDeclaration) {
            it.value
        }
        passthroughAnnotation<KScoped, Scoped>(builder, originKSClassDeclaration) {
            it.value
        }
    }

    @OptIn(KspExperimental::class, DelicateKotlinPoetApi::class)
    inline fun <reified T: Annotation, reified R: Annotation> passthroughAnnotation(
        builder: TypeSpec.Builder,
        originKSClassDeclaration: KSClassDeclaration,
        resolveValue: (T) -> R
    ) {
        val scopeParseResult = Result.runCatching {
            // this will work for Scope(name = "some string") but it will fail on Scope(value = SomeClass::class)
            originKSClassDeclaration.getAnnotationsByType(T::class).firstOrNull()?.also {
                val value = resolveValue(it)
                builder.addAnnotation(AnnotationSpec.get(value.extendProxy(), false))
            }
        }
        if (scopeParseResult.isFailure) {
            // this is a fallback method that will work on Scope(value = SomeClass::class)
            // since it fails when value is set to default we have to keep both approaches here
            originKSClassDeclaration.annotations
                .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == T::class.qualifiedName }
                .firstOrNull()
                ?.also {
                    it.arguments.firstOrNull()?.also { argument ->
                        val scope = argument.value as KSAnnotation
                        builder.addAnnotation(scope.toAnnotationSpec())
                    }
                }
        }
    }
}
