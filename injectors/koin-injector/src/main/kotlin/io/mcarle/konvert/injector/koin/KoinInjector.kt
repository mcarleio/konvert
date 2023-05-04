package io.mcarle.konvert.injector.koin

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.injector.koin.config.InjectionMethod
import io.mcarle.konvert.injector.koin.config.defaultInjectionMethod
import io.mcarle.konvert.injector.koin.config.defaultScope
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.plugin.api.passthroughAnnotation
import org.koin.core.annotation.*

@AutoService(KonverterInjector::class)
class KoinInjector : KonverterInjector {

    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        applyDefaults(builder)
        processAnnotations(builder, originKSClassDeclaration)
    }

    private fun applyDefaults(builder: TypeSpec.Builder) {
        when (Configuration.defaultInjectionMethod) {
            InjectionMethod.DISABLED -> return
            InjectionMethod.FACTORY -> builder.addAnnotation(Factory::class)
            InjectionMethod.SINGLE -> builder.addAnnotation(Single::class)
            InjectionMethod.SCOPE -> {
                val scopeString = Configuration.defaultScope
                require(scopeString.isNotBlank()) {
                    "The scope setting cannot be empty"
                }
                val annotationBuilder = AnnotationSpec.builder(Scope::class)
                val nameSegments = scopeString.split(".")
                if (nameSegments.count() == 1) {
                    annotationBuilder.addMember("name = %S", scopeString)
                } else {
                    val className = ClassName(nameSegments.dropLast(1).joinToString("."), nameSegments.last())
                    // at this point we are pretty sure that user passed qualified name
                    annotationBuilder.addMember("value = %T::class", className)
                }
                builder.addAnnotation(annotationBuilder.build())
            }
        }
    }

    private fun processAnnotations(
        builder: TypeSpec.Builder,
        originKSClassDeclaration: KSClassDeclaration
    ) {
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
}
