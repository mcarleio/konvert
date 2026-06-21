package io.mcarle.konvert.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import org.junit.jupiter.api.Nested
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for the annotation argument extension functions in `extensions.kt`.
 *
 * These reproduce the `kspCommonMainKotlinMetadata` (common-metadata) behavior that the JVM
 * `*ITest` integration harness cannot: in that phase KSP omits defaulted arguments from
 * [KSAnnotation.arguments] and returns `null` values for array-typed entries in
 * [KSAnnotation.defaultArguments]. We fake [KSAnnotation] to simulate exactly that and assert
 * the helpers resolve robustly instead of throwing [NoSuchElementException] / casting `null`.
 */
class ExtensionsTest {

    @Nested
    inner class ArgumentValue {

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
            val annotation = fakeAnnotation(
                arguments = emptyList(),
                defaultArguments = emptyList()
            )

            assertEquals("", annotation.argumentValue("source", fallback = ""))
        }

        @Test
        fun usesCallerFallbackWhenArrayDefaultComesBackNull() {
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
    }

    @Nested
    inner class ConstructorArgClassDeclarations {

        @Test
        fun returnsClassDeclarationsFromExplicitArgument() {
            val classDecl1 = proxy<KSClassDeclaration> { method -> unsupported(method) }
            val classDecl2 = proxy<KSClassDeclaration> { method -> unsupported(method) }
            val type1 = fakeKSType(classDecl1)
            val type2 = fakeKSType(classDecl2)

            val annotation = fakeAnnotation(
                arguments = listOf("converters" to listOf(type1, type2)),
                defaultArguments = emptyList()
            )

            val unitType = fakeKSType(null)
            val result = annotation.constructorArgClassDeclarations("converters", unitType)

            assertEquals(2, result.size)
            assertSame(classDecl1, result[0])
            assertSame(classDecl2, result[1])
        }

        @Test
        fun fallsBackToUnitTypeWhenArgumentAbsent() {
            val unitDecl = proxy<KSClassDeclaration> { method -> unsupported(method) }
            val unitType = fakeKSType(unitDecl)

            val annotation = fakeAnnotation(
                arguments = emptyList(),
                defaultArguments = emptyList()
            )

            val result = annotation.constructorArgClassDeclarations("converters", unitType)

            assertEquals(1, result.size)
            assertSame(unitDecl, result[0])
        }

        @Test
        fun filtersOutNonClassDeclarations() {
            val nonClassDecl = proxy<KSDeclaration> { method -> unsupported(method) }
            val typeWithNonClassDecl = fakeKSType(nonClassDecl)

            val annotation = fakeAnnotation(
                arguments = listOf("converters" to listOf(typeWithNonClassDecl)),
                defaultArguments = emptyList()
            )

            val unitType = fakeKSType(null)
            val result = annotation.constructorArgClassDeclarations("converters", unitType)

            assertTrue(result.isEmpty())
        }

        @Test
        fun returnsEmptyListForEmptyExplicitArgument() {
            val annotation = fakeAnnotation(
                arguments = listOf("converters" to emptyList<KSType>()),
                defaultArguments = emptyList()
            )

            val unitType = fakeKSType(null)
            val result = annotation.constructorArgClassDeclarations("converters", unitType)

            assertTrue(result.isEmpty())
        }

        @Test
        fun usesDefaultArgumentWhenExplicitAbsent() {
            val classDecl = proxy<KSClassDeclaration> { method -> unsupported(method) }
            val defaultType = fakeKSType(classDecl)

            val annotation = fakeAnnotation(
                arguments = emptyList(),
                defaultArguments = listOf("converters" to listOf(defaultType))
            )

            val unitType = fakeKSType(null)
            val result = annotation.constructorArgClassDeclarations("converters", unitType)

            assertEquals(1, result.size)
            assertSame(classDecl, result[0])
        }
    }

    // ── shared test infrastructure ──

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

    private fun fakeKSType(declaration: KSDeclaration?): KSType = proxy { method ->
        when (method.name) {
            "getDeclaration" -> declaration
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

