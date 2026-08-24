@file:JvmName("Checks")
@file:JvmMultifileClass

package top.likoslupus.cellulosesz.api.validation

public inline fun String.requireNonBlank(lazyName: () -> Any): String {
    require(isNotBlank()) {
        "${lazyName()} must not be blank"
    }
    return this
}

public fun requireNonBlank(value: String, name: String): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value.isNotBlank()) {
        "$name must not be blank"
    }
    return value
}

public inline fun String.requireNonEmpty(
    lazyName: () -> Any
): String {
    require(isNotEmpty()) {
        "${lazyName()} must not be empty"
    }
    return this
}

public fun requireNonEmpty(
    value: String,
    name: String
): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value.isNotEmpty()) {
        "$name must not be empty"
    }
    return value
}

public inline fun String.requireNoControlCharacters(
    lazyName: () -> Any
): String {
    var offset = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        require(!Character.isISOControl(codePoint)) {
            "${lazyName()} must not contain control character ${String.format("U+%04X", codePoint)}"
        }
        offset += Character.charCount(codePoint)
    }
    return this
}

public fun requireNoControlCharacters(
    value: String,
    name: String
): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    var offset = 0
    while (offset < value.length) {
        val codePoint = value.codePointAt(offset)
        require(!Character.isISOControl(codePoint)) {
            "$name must not contain control character ${String.format("U+%04X", codePoint)}"
        }
        offset += Character.charCount(codePoint)
    }
    return value
}

public inline fun String.requireMinLength(
    minimum: Int,
    lazyName: () -> Any
): String {
    require(minimum >= 0) {
        "minimum must be at least 0, but was $minimum"
    }
    require(length >= minimum) {
        "${lazyName()} length must be at least $minimum, but was $length"
    }
    return this
}

public fun requireMinLength(
    value: String,
    minimum: Int,
    name: String
): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(minimum >= 0) {
        "minimum must be at least 0, but was $minimum"
    }
    require(value.length >= minimum) {
        "$name length must be at least $minimum, but was ${value.length}"
    }
    return value
}

public inline fun String.requireMaxLength(
    maximum: Int,
    lazyName: () -> Any
): String {
    require(maximum >= 0) {
        "maximum must be at least 0, but was $maximum"
    }
    require(length <= maximum) {
        "${lazyName()} length must be at most $maximum, but was $length"
    }
    return this
}

public fun requireMaxLength(
    value: String,
    maximum: Int,
    name: String
): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(maximum >= 0) {
        "maximum must be at least 0, but was $maximum"
    }
    require(value.length <= maximum) {
        "$name length must be at most $maximum, but was ${value.length}"
    }
    return value
}

public inline fun String.requireLengthInRange(
    minimum: Int,
    maximum: Int,
    lazyName: () -> Any
): String {
    require(minimum <= maximum) {
        "${lazyName()} length minimum must not exceed maximum"
    }
    require(minimum >= 0) {
        "minimum must be at least 0, but was $minimum"
    }
    require(length in minimum..maximum) {
        "${lazyName()} length must be in [$minimum, $maximum], but was $length"
    }
    return this
}

public fun requireLengthInRange(
    value: String,
    minimum: Int,
    maximum: Int,
    name: String
): String {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(minimum <= maximum) {
        "$name length minimum must not exceed maximum"
    }
    require(minimum >= 0) {
        "minimum must be at least 0, but was $minimum"
    }
    require(value.length in minimum..maximum) {
        "$name length must be in [$minimum, $maximum], but was ${value.length}"
    }
    return value
}
