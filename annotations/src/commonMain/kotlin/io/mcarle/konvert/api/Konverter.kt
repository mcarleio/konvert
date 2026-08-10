package io.mcarle.konvert.api

import kotlin.reflect.KClass

/**
 * Annotate an interface with Konvert annotated functions to generate an implementation of it.
 *
 * Example:
 * ```kotlin
 * class Source(val source: Int)
 * class Target(val target: String)
 *
 * @Konverter
 * interface Mapper {
 *   @Konvert(mappings = [Mapping(source="source", target="target")])
 *   fun toTarget(source: Source): Target
 * }
 * ```
 *
 * This will generate an implementation object of the interface in the same package:
 * ```kotlin
 * object MapperImpl : Mapper {
 *      override fun toTarget(source: Source): Target = Target(target = source.source.toString())
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Konverter(
    val options: Array<Konfig> = []
) {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Source

    companion object {
        const val KONVERTER_GENERATED_CLASS_SUFFIX = "Impl"

        @Deprecated(
            message = "Moved to JVM part. Will be removed in upcoming releases",
            replaceWith = ReplaceWith(
                "addJvmClassLoader(classLoader)",
                "io.mcarle.konvert.api.addJvmClassLoader"
            )
        )
        fun addClassLoader(classLoader: Any) {
            deprecated_addClassLoader(classLoader)
        }

        @Deprecated(
            message = "Moved to JVM part. Will be removed in upcoming releases",
            replaceWith = ReplaceWith(
                "removeJvmClassLoader(classLoader)",
                "io.mcarle.konvert.api.removeJvmClassLoader"
            )
        )
        fun removeClassLoader(classLoader: Any) {
            deprecated_removeClassLoader(classLoader)
        }

        inline fun <reified T : Any> get(): T = get(T::class)

        fun <T : Any> get(clazz: KClass<T>): T {
            return loadKonverterViaReflection(clazz)
        }

    }
}

expect fun <T : Any> loadKonverterViaReflection(clazz: KClass<T>): T

expect fun deprecated_addClassLoader(classLoader: Any)

expect fun deprecated_removeClassLoader(classLoader: Any)
