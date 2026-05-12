package io.mcarle.konvert.api

import kotlin.reflect.KClass

actual fun <T : Any> loadKonverterViaReflection(clazz: KClass<T>): T {
    throw RuntimeException("Reflection is not supported on JS, please disable the option 'konvert.konverter.use-reflection'")
}

actual fun deprecated_addClassLoader(classLoader: Any) {
    throw RuntimeException("ClassLoader not supported on JS, please do not use `addClassLoader`")
}

actual fun deprecated_removeClassLoader(classLoader: Any) {
    throw RuntimeException("ClassLoader not supported on JS, please do not use `removeClassLoader`")
}
