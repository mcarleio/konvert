package io.mcarle.konvert.plugin.api

import java.lang.reflect.Proxy

inline fun <reified T : Annotation> T.extendProxy(): T {
    return if (Proxy.isProxyClass(this::class.java)) {
        val ih = Proxy.getInvocationHandler(this)
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { proxy, method, args ->
            if (method.name == "annotationType") {
                T::class.java
            } else {
                ih.invoke(proxy, method, args)
            }
        } as T
    } else {
        this
    }
}
