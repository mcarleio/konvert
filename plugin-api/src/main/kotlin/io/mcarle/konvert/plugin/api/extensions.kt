package io.mcarle.konvert.plugin.api

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import java.lang.reflect.Proxy

inline fun <reified T : Annotation> T.extendProxy(): T {
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

@OptIn(KspExperimental::class, DelicateKotlinPoetApi::class)
inline fun <reified T: Annotation, reified R: Annotation> passthroughAnnotation(
    builder: TypeSpec.Builder,
    originKSClassDeclaration: KSClassDeclaration,
    resolveValue: (T) -> R
): Boolean {
    val scopeParseResult = Result.runCatching {
        // this will work for Scope(name = "some string") but it will fail on Scope(value = SomeClass::class)
        originKSClassDeclaration.getAnnotationsByType(T::class).firstOrNull()?.also {
            val value = resolveValue(it)
            builder.addAnnotation(AnnotationSpec.get(value.extendProxy(), false))
            return true
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
                    return true
                }
            }
    }
    return false
}
