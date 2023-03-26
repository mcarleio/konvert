package io.mcarle.kmap.api

import kotlin.reflect.KClass

/**
 * This object can be used to load the generated class of an interface, which is annotated with `@KMapper`.
 */
@Suppress("UNCHECKED_CAST")
object KMappers {

    private val mappers: MutableMap<KClass<*>, Any> = mutableMapOf()
    private val classLoader = mutableListOf(
        ClassLoader.getSystemClassLoader()
    )

    inline fun <reified T : Any> get(): T = get(T::class)
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