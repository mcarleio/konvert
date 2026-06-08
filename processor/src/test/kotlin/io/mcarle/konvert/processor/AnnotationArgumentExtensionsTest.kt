package io.mcarle.konvert.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSValueArgument
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [argumentValue].
 *
 * These reproduce the `kspCommonMainKotlinMetadata` (common-metadata) behavior that the JVM
 * `*ITest` integration harness cannot: in that phase KSP omits defaulted arguments from
 * [KSAnnotation.arguments] and returns `null` values for array-typed entries in
 * [KSAnnotation.defaultArguments]. We fake [KSAnnotation] to simulate exactly that and assert
 * the helper resolves robustly instead of throwing [NoSuchElementException] / casting `null`.
 */
class AnnotationArgumentExtensionsTest {

    @Test
    fun returnsExplicitArgumentValueWhenPresent() {
        val annotation = fakeAnnotation(
            arguments = listOf("target" to "Foo"),
            defaultArguments = emptyList()
        )

        assertEquals("Foo", annotation.argumentValue("target", fallback = "fallback"))
    }

    @Test
    fun fallsBackToDefaultArgumentWhenNotExplicitlySet() {
        val annotation = fakeAnnotation(
            arguments = emptyList(),
            defaultArguments = listOf("priority" to 42)
        )

        assertEquals(42, annotation.argumentValue("priority", fallback = -1))
    }

    @Test
    fun usesCallerFallbackWhenArgumentIsAbsentEntirely() {
        // Simulates a defaulted argument that the metadata phase drops from both lists.
        val annotation = fakeAnnotation(
            arguments = emptyList(),
            defaultArguments = emptyList()
        )

        assertEquals("", annotation.argumentValue("source", fallback = ""))
    }

    @Test
    fun usesCallerFallbackWhenArrayDefaultComesBackNull() {
        // In the metadata phase array-valued defaults (e.g. `mappings = []`) materialize with a
        // null value; the helper must not return that null (which would NPE on `as List<*>`).
        val fallback = emptyList<Any?>()
        val annotation = fakeAnnotation(
            arguments = emptyList(),
            defaultArguments = listOf("mappings" to null)
        )

        assertSame(fallback, annotation.argumentValue("mappings", fallback = fallback))
    }

    @Test
    fun prefersExplicitArgumentOverDefault() {
        val annotation = fakeAnnotation(
            arguments = listOf("priority" to 7),
            defaultArguments = listOf("priority" to 42)
        )

        assertEquals(7, annotation.argumentValue("priority", fallback = -1))
    }

    @Test
    fun ignoresExplicitNullValueAndFallsThrough() {
        val annotation = fakeAnnotation(
            arguments = listOf("priority" to null),
            defaultArguments = listOf("priority" to 42)
        )

        assertEquals(42, annotation.argumentValue("priority", fallback = -1))
    }

    private fun fakeAnnotation(
        arguments: List<Pair<String, Any?>>,
        defaultArguments: List<Pair<String, Any?>>
    ): KSAnnotation {
        val args = arguments.map { (name, value) -> fakeValueArgument(name, value) }
        val defaults = defaultArguments.map { (name, value) -> fakeValueArgument(name, value) }
        return proxy { method ->
            when (method.name) {
                "getArguments" -> args
                "getDefaultArguments" -> defaults
                else -> unsupported(method)
            }
        }
    }

    private fun fakeValueArgument(name: String, value: Any?): KSValueArgument = proxy { method ->
        when (method.name) {
            "getName" -> fakeName(name)
            "getValue" -> value
            else -> unsupported(method)
        }
    }

    private fun fakeName(value: String): KSName = proxy { method ->
        when (method.name) {
            "asString" -> value
            else -> unsupported(method)
        }
    }

    private inline fun <reified T> proxy(crossinline handler: (Method) -> Any?): T {
        val invocationHandler = InvocationHandler { _, method, _ -> handler(method) }
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), invocationHandler) as T
    }

    private fun unsupported(method: Method): Nothing =
        throw UnsupportedOperationException("Fake does not implement ${method.name}")
}
