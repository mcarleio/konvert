package io.mcarle.lib.kmapper.annotation

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KMapFromExt(
    val from: Array<KMapFrom> = [],
    val extName: String = ""
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KMapToExt(
    val to: Array<KMapTo> = [],
    val extName: String = ""
)

@Retention(AnnotationRetention.SOURCE)
annotation class KMapTo(
    val value: KClass<*>,
    val mappings: Array<KMap> = []
)
typealias KMapFrom = KMapTo



@KMapToExt(
    to = [KMapTo(B::class, mappings = [KMap("x", "a"), KMap("y", "z")])]
)
data class A(
    val x: Int,
    val y: C,
)

data class B(
    val a: Int,
    val z: D,
) {
    @KMapFromExt(
        from = [KMapFrom(A::class, mappings = [KMap("x", "a"), KMap("y", "z")])]
    )
    companion object
}

@KMapToExt(to = [KMapTo(D::class)])
data class C(
    val s: String
)

data class D(
    val s: String
)

// Will generate:
fun A.mapToB(): B = B(a = x, z = y.mapToD())
fun B.Companion.mapFromA(a: A): B = B(a = a.x, z = a.y.mapToD())
fun C.mapToD(): D = D(s = s)

