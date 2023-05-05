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

/**
 * This function is using convention of "Wrapper Annotation" to passthrough annotation from interface to impl class.
 *
 * Assuming given annotation:
 * ```kotlin
 * @Retention(AnnotationRetention.RUNTIME)
 * @Target(AnnotationTarget.CLASS)
 * annotation class KSingleton(
 *     val value: Singleton = Singleton(),
 * )
 * ```
 *
 * and given interface:
 *
 * ```kotlin
 * @KSingleton
 * interface SomeMapper ...
 * ```
 *
 * we expect to end up with impl class like:
 *
 * ```kotlin
 * @Singleton
 * class SomeMapperImpl ...
 * ```
 *
 * This function is doing that automatically. We are assuming that the parameter in the wrapper has only a single argument
 * and that argument is annotation we want to apply to impl class.
 *
 * We are using 2 approaches. First - we try to resolve the annotation directly. If it is successfull the [resolveValue]
 * function is called to access underlying annotation. This approach might fail if the annotation is using KClass parameter
 * like `Scope(value = SomeClass::class)`
 *
 * @param T Wrapper annotation
 * @param R Annotation we want to apply to impl class
 * @param builder Will be used to add annotation to the impl class
 * @param originKSClassDeclaration Impl class declaration
 * @param resolveValue Function that has to point to [R] annotation from [T] parameters in order to resolve it properly
 *
 * @return true if annotation was properly added to [builder]. False if the [T] annotation couldn't be find
 *
 * @sample [io.mcarle.konvert.injector.koin.KoinInjector.processAnnotations]
 */
@OptIn(KspExperimental::class, DelicateKotlinPoetApi::class)
inline fun <reified T: Annotation, reified R: Annotation> passthroughAnnotation(
    builder: TypeSpec.Builder,
    originKSClassDeclaration: KSClassDeclaration,
    resolveValue: (T) -> R
): Boolean {
    val annotationResolveResult = Result.runCatching {
        // Let's try to resolve [T] annotation. If that fails it suggests that some of the [R] parameters has [KClass] type
        originKSClassDeclaration.getAnnotationsByType(T::class).firstOrNull()?.also {
            val value = resolveValue(it)
            builder.addAnnotation(AnnotationSpec.get(value.extendProxy(), false))
            return true
        }
    }
    if (annotationResolveResult.isFailure) {
        // if the annotation couldn't be resolved we have to passthrough annotation directly without resolving it
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
