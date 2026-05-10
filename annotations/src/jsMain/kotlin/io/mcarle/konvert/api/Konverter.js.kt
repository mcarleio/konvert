package io.mcarle.konvert.api

import kotlin.reflect.KClass

actual fun <T : Any> loadKonverterViaReflection(clazz: KClass<T>): T {
    throw RuntimeException("Reflection is not supported on JS, please disable the option 'konvert.konverter.use-reflection'")
}
