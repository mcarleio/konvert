package io.mcarle.konvert.api

import kotlin.reflect.KClass

private val mappers: MutableMap<KClass<*>, Any> = mutableMapOf()
private val CLASS_LOADER_LIST = mutableListOf(ClassLoader.getSystemClassLoader())

fun addClassLoader(classLoader: ClassLoader) {
    CLASS_LOADER_LIST += classLoader
}

fun removeClassLoader(classLoader: ClassLoader) {
    CLASS_LOADER_LIST -= classLoader
}

private inline fun <T: Any> withCurrentClassLoaders(clazz: KClass<T>, block: (List<ClassLoader>) -> T): T {
    return block(
        listOfNotNull(clazz.java.classLoader, Thread.currentThread().contextClassLoader, *CLASS_LOADER_LIST.toTypedArray())
    )
}

@Suppress("UNCHECKED_CAST")
actual fun <T : Any> loadKonverterViaReflection(clazz: KClass<T>): T {
    return withCurrentClassLoaders(clazz) { classLoaders ->
        if (!mappers.containsKey(clazz)) {
            val implFQN = "${clazz.qualifiedName}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}"
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
