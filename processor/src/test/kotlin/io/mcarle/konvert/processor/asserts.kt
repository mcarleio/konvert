package io.mcarle.konvert.processor

import kotlin.test.fail

fun assertDoesNotContain(
    actual: String,
    unexpected: String,
    message: String = "Expected '$actual' to not contain '$unexpected'"
) {
    if (actual.contains(unexpected)) {
        fail(message)
    }
}
