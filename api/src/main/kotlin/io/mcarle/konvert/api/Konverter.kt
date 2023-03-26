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
annotation class Konverter {
    /**
     * This object can be used to load the generated class of an interface, which is annotated with `@Konverter`.
     */
    companion object {
        private val mappers: MutableMap<KClass<*>, Any> = mutableMapOf()
        private val classLoader = mutableListOf(
            ClassLoader.getSystemClassLoader()
        )

        inline fun <reified T : Any> get(): T = get(T::class)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> get(clazz: KClass<T>): T {
            if (!mappers.containsKey(clazz)) {
                mappers[clazz] = classLoader.firstNotNullOf {
                    try {
                        it.loadClass("${clazz.qualifiedName}Impl")
                    } catch (e: Exception) {
                        null
                    }
                }.getDeclaredField("INSTANCE").get(null)
//            }.kotlin.objectInstance!! // needs kotlin-reflect during runtime
            }
            return mappers[clazz] as T
        }
    }
}
