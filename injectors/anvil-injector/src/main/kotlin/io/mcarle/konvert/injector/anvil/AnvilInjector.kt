package io.mcarle.konvert.injector.anvil

import com.google.auto.service.AutoService
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import dagger.MapKey
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.injector.anvil.config.InjectionMethod
import io.mcarle.konvert.injector.anvil.config.defaultInjectionMethod
import io.mcarle.konvert.injector.anvil.config.defaultScope
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.plugin.api.passthroughAnnotation
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@AutoService(KonverterInjector::class)
class AnvilInjector : KonverterInjector {
    override fun processType(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        val defaultInjectionApplied = applyDefaults(builder)

        val bindingUsed = passthroughAnnotation<KContributesBinding, ContributesBinding>(builder, originKSClassDeclaration) {
            it.value
        }
        val multibindingUsed = passthroughAnnotation<KContributesMultibinding, ContributesMultibinding>(builder, originKSClassDeclaration) {
            it.value
        }
        passthroughAnnotation<KSingleton, Singleton>(builder, originKSClassDeclaration) {
            it.value
        }

        handleQualifiersAndMapKeys(builder, originKSClassDeclaration)

        if (defaultInjectionApplied || bindingUsed || multibindingUsed) {
            // We also have to add empty constructor with @Inject annotation
            builder.addFunction(FunSpec.constructorBuilder()
                .addAnnotation(Inject::class)
                .build()
            )
        }
    }

    private fun applyDefaults(builder: TypeSpec.Builder): Boolean {
        val defaultInjectionMethod = Configuration.defaultInjectionMethod
        if (defaultInjectionMethod == InjectionMethod.DISABLED) return false

        val scopeString = Configuration.defaultScope
        require(scopeString.isNotBlank()) {
            "The scope setting cannot be empty"
        }
        val scopeClassName = ClassName.bestGuess(scopeString)
        builder.addAnnotation(AnnotationSpec.builder(ContributesBinding::class)
            .addMember("scope = %T::class", scopeClassName)
            .build())

        if (defaultInjectionMethod == InjectionMethod.SINGLETON){
            builder.addAnnotation(Singleton::class)
        }
        return true
    }

    @OptIn(KspExperimental::class)
    private fun handleQualifiersAndMapKeys(builder: TypeSpec.Builder, originKSClassDeclaration: KSClassDeclaration) {
        originKSClassDeclaration.annotations
            .filter {
                // look for all annotations that are qualifiers or MapKeys
                val resolvedType = it.annotationType.resolve()
                val isQualifier = resolvedType
                    .classDeclaration()
                    ?.isAnnotationPresent(Qualifier::class) ?: false
                val isMapKey = resolvedType
                    .classDeclaration()
                    ?.isAnnotationPresent(MapKey::class) ?: false
                isQualifier || isMapKey
            }
            .forEach {
                builder.addAnnotation(it.toAnnotationSpec())
            }
    }
}
