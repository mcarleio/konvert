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

        inline fun <reified T : Any> get(): T = get(T::class)

        fun <T : Any> get(clazz: KClass<T>): T {
            return loadKonverterViaReflection(clazz)
        }

    }
}
expect fun <T: Any> loadKonverterViaReflection(clazz: KClass<T>): T
