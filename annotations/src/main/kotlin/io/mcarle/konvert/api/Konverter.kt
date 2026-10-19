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

    /**
     * Marks the parameter to map from, when a function has further parameters. Those are mapped to the same named target
     * properties as they are.
     *
     * Several parameters can be marked to map all of them into one target: a target property is then matched by name
     * against the properties of all sources, and must be decided with a [Mapping] if more than one source has it.
     */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.VALUE_PARAMETER)
    annotation class Source

    /**
     * This object can be used to load the generated class of an interface, which is annotated with `@Konverter`.
     */
    companion object {
        const val KONVERTER_GENERATED_CLASS_SUFFIX = "Impl"

        private val mappers: MutableMap<KClass<*>, Any> = mutableMapOf()
        private val CLASS_LOADER_LIST = mutableListOf(
            ClassLoader.getSystemClassLoader()
        )

        fun addClassLoader(classLoader: ClassLoader) {
            this.CLASS_LOADER_LIST += classLoader
        }

        fun removeClassLoader(classLoader: ClassLoader) {
            this.CLASS_LOADER_LIST -= classLoader
        }

        inline fun <reified T : Any> get(): T = get(T::class)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> get(clazz: KClass<T>): T {
            return withCurrentClassLoaders(clazz) { classLoaders ->
                if (!mappers.containsKey(clazz)) {
                    val implFQN = "${clazz.qualifiedName}$KONVERTER_GENERATED_CLASS_SUFFIX"
                    val implClass = classLoaders.firstNotNullOfOrNull {
                        try {
                            it.loadClass(implFQN)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: throw RuntimeException("Could not load the class $implFQN from provided class loaders")

                    var implInstance = implClass.declaredFields.firstOrNull {
                        it.name == "INSTANCE"
                    }?.get(null)

                    if (implInstance == null) {
                        implInstance = implClass.constructors.firstOrNull { it.parameterTypes.isEmpty() }?.newInstance()
                            ?: throw RuntimeException("Could not determine INSTANCE or empty constructor for $implClass")
                    }
                    mappers[clazz] = implInstance
                }
                return mappers[clazz] as T
            }
        }

        private inline fun <T: Any> withCurrentClassLoaders(clazz: KClass<T>, block: (List<ClassLoader>) -> T): T {
            return block(
                listOfNotNull(clazz.java.classLoader, Thread.currentThread().contextClassLoader, *CLASS_LOADER_LIST.toTypedArray())
            )
        }
    }
}
