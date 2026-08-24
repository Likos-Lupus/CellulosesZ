@file:JvmName("Checks")
@file:JvmMultifileClass

package top.likoslupus.cellulosesz.api.validation

import java.math.BigDecimal
import java.time.Duration

public inline fun Float.requireFinite(lazyName: () -> Any): Float {
    require(java.lang.Float.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    return this
}

public fun requireFinite(value: Float, name: String): Float {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Float.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    return value
}

public inline fun Double.requireFinite(lazyName: () -> Any): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    return this
}

public fun requireFinite(value: Double, name: String): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    return value
}

public inline fun Int.requirePositive(lazyName: () -> Any): Int {
    require(this > 0) {
        "${lazyName()} must be greater than 0, but was $this"
    }
    return this
}

public fun requirePositive(value: Int, name: String): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value > 0) {
        "$name must be greater than 0, but was $value"
    }
    return value
}

public inline fun Long.requirePositive(lazyName: () -> Any): Long {
    require(this > 0L) {
        "${lazyName()} must be greater than 0, but was $this"
    }
    return this
}

public fun requirePositive(value: Long, name: String): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value > 0L) {
        "$name must be greater than 0, but was $value"
    }
    return value
}

public inline fun Double.requirePositive(lazyName: () -> Any): Double {
    require(java.lang.Double.isFinite(this) && this > 0.0) {
        "${lazyName()} must be greater than 0, but was $this"
    }
    return this
}

public fun requirePositive(value: Double, name: String): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value) && value > 0.0) {
        "$name must be greater than 0, but was $value"
    }
    return value
}

public inline fun BigDecimal.requirePositive(lazyName: () -> Any): BigDecimal {
    require(signum() > 0) {
        "${lazyName()} must be greater than 0, but was $this"
    }
    return this
}

public fun requirePositive(value: BigDecimal, name: String): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value.signum() > 0) {
        "$name must be greater than 0, but was $value"
    }
    return value
}

public inline fun Duration.requirePositive(lazyName: () -> Any): Duration {
    require(!isNegative && !isZero) {
        "${lazyName()} must be greater than 0, but was $this"
    }
    return this
}

public fun requirePositive(value: Duration, name: String): Duration {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(!value.isNegative && !value.isZero) {
        "$name must be greater than 0, but was $value"
    }
    return value
}

public inline fun Int.requireNonNegative(lazyName: () -> Any): Int {
    require(this >= 0) {
        "${lazyName()} must be at least 0, but was $this"
    }
    return this
}

public fun requireNonNegative(value: Int, name: String): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value >= 0) {
        "$name must be at least 0, but was $value"
    }
    return value
}

public inline fun Long.requireNonNegative(lazyName: () -> Any): Long {
    require(this >= 0L) {
        "${lazyName()} must be at least 0, but was $this"
    }
    return this
}

public fun requireNonNegative(value: Long, name: String): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value >= 0L) {
        "$name must be at least 0, but was $value"
    }
    return value
}

public inline fun Double.requireNonNegative(lazyName: () -> Any): Double {
    require(java.lang.Double.isFinite(this) && this >= 0.0) {
        "${lazyName()} must be at least 0, but was $this"
    }
    return this
}

public fun requireNonNegative(value: Double, name: String): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value) && value >= 0.0) {
        "$name must be at least 0, but was $value"
    }
    return value
}

public inline fun BigDecimal.requireNonNegative(lazyName: () -> Any): BigDecimal {
    require(signum() >= 0) {
        "${lazyName()} must be at least 0, but was $this"
    }
    return this
}

public fun requireNonNegative(value: BigDecimal, name: String): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value.signum() >= 0) {
        "$name must be at least 0, but was $value"
    }
    return value
}

public inline fun Duration.requireNonNegative(lazyName: () -> Any): Duration {
    require(!isNegative) {
        "${lazyName()} must be at least 0, but was $this"
    }
    return this
}

public fun requireNonNegative(value: Duration, name: String): Duration {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(!value.isNegative) {
        "$name must be at least 0, but was $value"
    }
    return value
}

public inline fun Int.requirePositiveOrNegativeOne(lazyName: () -> Any): Int {
    require(this == -1 || this > 0) {
        "${lazyName()} must be positive or equal to -1, but was $this"
    }
    return this
}

public fun requirePositiveOrNegativeOne(value: Int, name: String): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value == -1 || value > 0) {
        "$name must be positive or equal to -1, but was $value"
    }
    return value
}

public inline fun Long.requirePositiveOrNegativeOne(lazyName: () -> Any): Long {
    require(this == -1L || this > 0L) {
        "${lazyName()} must be positive or equal to -1, but was $this"
    }
    return this
}

public fun requirePositiveOrNegativeOne(value: Long, name: String): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value == -1L || value > 0L) {
        "$name must be positive or equal to -1, but was $value"
    }
    return value
}
