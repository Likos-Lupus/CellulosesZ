@file:JvmName("Checks")
@file:JvmMultifileClass

package top.likoslupus.cellulosesz.api.validation

import java.math.BigDecimal

public inline fun Int.requireInRange(
    minimum: Int,
    maximum: Int,
    lazyName: () -> Any
): Int {
    require(minimum <= maximum) {
        "${lazyName()} minimum must not exceed maximum"
    }
    require(this in minimum..maximum) {
        "${lazyName()} must be in [$minimum, $maximum], but was $this"
    }
    return this
}

public fun requireInRange(
    value: Int,
    minimum: Int,
    maximum: Int,
    name: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(minimum <= maximum) {
        "$name minimum must not exceed maximum"
    }
    require(value in minimum..maximum) {
        "$name must be in [$minimum, $maximum], but was $value"
    }
    return value
}

public inline fun Long.requireInRange(
    minimum: Long,
    maximum: Long,
    lazyName: () -> Any
): Long {
    require(minimum <= maximum) {
        "${lazyName()} minimum must not exceed maximum"
    }
    require(this in minimum..maximum) {
        "${lazyName()} must be in [$minimum, $maximum], but was $this"
    }
    return this
}

public fun requireInRange(
    value: Long,
    minimum: Long,
    maximum: Long,
    name: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(minimum <= maximum) {
        "$name minimum must not exceed maximum"
    }
    require(value in minimum..maximum) {
        "$name must be in [$minimum, $maximum], but was $value"
    }
    return value
}

public inline fun Double.requireInRange(
    minimum: Double,
    maximum: Double,
    lazyName: () -> Any
): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    require(java.lang.Double.isFinite(minimum)) {
        "${lazyName()} minimum must be finite, but was $minimum"
    }
    require(java.lang.Double.isFinite(maximum)) {
        "${lazyName()} maximum must be finite, but was $maximum"
    }
    require(minimum <= maximum) {
        "${lazyName()} minimum must not exceed maximum"
    }
    require(this in minimum..maximum) {
        "${lazyName()} must be in [$minimum, $maximum], but was $this"
    }
    return this
}

public fun requireInRange(
    value: Double,
    minimum: Double,
    maximum: Double,
    name: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(minimum)) {
        "$name minimum must be finite, but was $minimum"
    }
    require(java.lang.Double.isFinite(maximum)) {
        "$name maximum must be finite, but was $maximum"
    }
    require(minimum <= maximum) {
        "$name minimum must not exceed maximum"
    }
    require(value in minimum..maximum) {
        "$name must be in [$minimum, $maximum], but was $value"
    }
    return value
}

public inline fun BigDecimal.requireInRange(
    minimum: BigDecimal,
    maximum: BigDecimal,
    lazyName: () -> Any
): BigDecimal {
    require(minimum <= maximum) {
        "${lazyName()} minimum must not exceed maximum"
    }
    require(this in minimum..maximum) {
        "${lazyName()} must be in [$minimum, $maximum], but was $this"
    }
    return this
}

public fun requireInRange(
    value: BigDecimal,
    minimum: BigDecimal,
    maximum: BigDecimal,
    name: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(minimum <= maximum) {
        "$name minimum must not exceed maximum"
    }
    require(value in minimum..maximum) {
        "$name must be in [$minimum, $maximum], but was $value"
    }
    return value
}

public inline fun Int.requireGreaterThan(
    minimumExclusive: Int,
    lazyName: () -> Any
): Int {
    require(this > minimumExclusive) {
        "${lazyName()} must be greater than $minimumExclusive, but was $this"
    }
    return this
}

public fun requireGreaterThan(
    value: Int,
    minimumExclusive: Int,
    name: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value > minimumExclusive) {
        "$name must be greater than $minimumExclusive, but was $value"
    }
    return value
}

public fun requireGreaterThan(
    value: Int,
    name: String,
    other: Int,
    otherName: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value > other) {
        "$name must be greater than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Long.requireGreaterThan(
    minimumExclusive: Long,
    lazyName: () -> Any
): Long {
    require(this > minimumExclusive) {
        "${lazyName()} must be greater than $minimumExclusive, but was $this"
    }
    return this
}

public fun requireGreaterThan(
    value: Long,
    minimumExclusive: Long,
    name: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value > minimumExclusive) {
        "$name must be greater than $minimumExclusive, but was $value"
    }
    return value
}

public fun requireGreaterThan(
    value: Long,
    name: String,
    other: Long,
    otherName: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value > other) {
        "$name must be greater than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Double.requireGreaterThan(
    minimumExclusive: Double,
    lazyName: () -> Any
): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    require(java.lang.Double.isFinite(minimumExclusive)) {
        "${lazyName()} minimumExclusive must be finite, but was $minimumExclusive"
    }
    require(this > minimumExclusive) {
        "${lazyName()} must be greater than $minimumExclusive, but was $this"
    }
    return this
}

public fun requireGreaterThan(
    value: Double,
    minimumExclusive: Double,
    name: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(minimumExclusive)) {
        "$name minimumExclusive must be finite, but was $minimumExclusive"
    }
    require(value > minimumExclusive) {
        "$name must be greater than $minimumExclusive, but was $value"
    }
    return value
}

public fun requireGreaterThan(
    value: Double,
    name: String,
    other: Double,
    otherName: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(other)) {
        "$otherName must be finite, but was $other"
    }
    require(value > other) {
        "$name must be greater than $otherName ($other), but was $value"
    }
    return value
}

public inline fun BigDecimal.requireGreaterThan(
    minimumExclusive: BigDecimal,
    lazyName: () -> Any
): BigDecimal {
    require(this > minimumExclusive) {
        "${lazyName()} must be greater than $minimumExclusive, but was $this"
    }
    return this
}

public fun requireGreaterThan(
    value: BigDecimal,
    minimumExclusive: BigDecimal,
    name: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value > minimumExclusive) {
        "$name must be greater than $minimumExclusive, but was $value"
    }
    return value
}

public fun requireGreaterThan(
    value: BigDecimal,
    name: String,
    other: BigDecimal,
    otherName: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value > other) {
        "$name must be greater than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Int.requireAtLeast(
    minimumInclusive: Int,
    lazyName: () -> Any
): Int {
    require(this >= minimumInclusive) {
        "${lazyName()} must be at least $minimumInclusive, but was $this"
    }
    return this
}

public fun requireAtLeast(
    value: Int,
    minimumInclusive: Int,
    name: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value >= minimumInclusive) {
        "$name must be at least $minimumInclusive, but was $value"
    }
    return value
}

public fun requireAtLeast(
    value: Int,
    name: String,
    other: Int,
    otherName: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value >= other) {
        "$name must be at least $otherName ($other), but was $value"
    }
    return value
}

public inline fun Long.requireAtLeast(
    minimumInclusive: Long,
    lazyName: () -> Any
): Long {
    require(this >= minimumInclusive) {
        "${lazyName()} must be at least $minimumInclusive, but was $this"
    }
    return this
}

public fun requireAtLeast(
    value: Long,
    minimumInclusive: Long,
    name: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value >= minimumInclusive) {
        "$name must be at least $minimumInclusive, but was $value"
    }
    return value
}

public fun requireAtLeast(
    value: Long,
    name: String,
    other: Long,
    otherName: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value >= other) {
        "$name must be at least $otherName ($other), but was $value"
    }
    return value
}

public inline fun Double.requireAtLeast(
    minimumInclusive: Double,
    lazyName: () -> Any
): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    require(java.lang.Double.isFinite(minimumInclusive)) {
        "${lazyName()} minimumInclusive must be finite, but was $minimumInclusive"
    }
    require(this >= minimumInclusive) {
        "${lazyName()} must be at least $minimumInclusive, but was $this"
    }
    return this
}

public fun requireAtLeast(
    value: Double,
    minimumInclusive: Double,
    name: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(minimumInclusive)) {
        "$name minimumInclusive must be finite, but was $minimumInclusive"
    }
    require(value >= minimumInclusive) {
        "$name must be at least $minimumInclusive, but was $value"
    }
    return value
}

public fun requireAtLeast(
    value: Double,
    name: String,
    other: Double,
    otherName: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(other)) {
        "$otherName must be finite, but was $other"
    }
    require(value >= other) {
        "$name must be at least $otherName ($other), but was $value"
    }
    return value
}

public inline fun BigDecimal.requireAtLeast(
    minimumInclusive: BigDecimal,
    lazyName: () -> Any
): BigDecimal {
    require(this >= minimumInclusive) {
        "${lazyName()} must be at least $minimumInclusive, but was $this"
    }
    return this
}

public fun requireAtLeast(
    value: BigDecimal,
    minimumInclusive: BigDecimal,
    name: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value >= minimumInclusive) {
        "$name must be at least $minimumInclusive, but was $value"
    }
    return value
}

public fun requireAtLeast(
    value: BigDecimal,
    name: String,
    other: BigDecimal,
    otherName: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value >= other) {
        "$name must be at least $otherName ($other), but was $value"
    }
    return value
}

public inline fun Int.requireLessThan(
    maximumExclusive: Int,
    lazyName: () -> Any
): Int {
    require(this < maximumExclusive) {
        "${lazyName()} must be less than $maximumExclusive, but was $this"
    }
    return this
}

public fun requireLessThan(
    value: Int,
    maximumExclusive: Int,
    name: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value < maximumExclusive) {
        "$name must be less than $maximumExclusive, but was $value"
    }
    return value
}

public fun requireLessThan(
    value: Int,
    name: String,
    other: Int,
    otherName: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value < other) {
        "$name must be less than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Long.requireLessThan(
    maximumExclusive: Long,
    lazyName: () -> Any
): Long {
    require(this < maximumExclusive) {
        "${lazyName()} must be less than $maximumExclusive, but was $this"
    }
    return this
}

public fun requireLessThan(
    value: Long,
    maximumExclusive: Long,
    name: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value < maximumExclusive) {
        "$name must be less than $maximumExclusive, but was $value"
    }
    return value
}

public fun requireLessThan(
    value: Long,
    name: String,
    other: Long,
    otherName: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value < other) {
        "$name must be less than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Double.requireLessThan(
    maximumExclusive: Double,
    lazyName: () -> Any
): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    require(java.lang.Double.isFinite(maximumExclusive)) {
        "${lazyName()} maximumExclusive must be finite, but was $maximumExclusive"
    }
    require(this < maximumExclusive) {
        "${lazyName()} must be less than $maximumExclusive, but was $this"
    }
    return this
}

public fun requireLessThan(
    value: Double,
    maximumExclusive: Double,
    name: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(maximumExclusive)) {
        "$name maximumExclusive must be finite, but was $maximumExclusive"
    }
    require(value < maximumExclusive) {
        "$name must be less than $maximumExclusive, but was $value"
    }
    return value
}

public fun requireLessThan(
    value: Double,
    name: String,
    other: Double,
    otherName: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(other)) {
        "$otherName must be finite, but was $other"
    }
    require(value < other) {
        "$name must be less than $otherName ($other), but was $value"
    }
    return value
}

public inline fun BigDecimal.requireLessThan(
    maximumExclusive: BigDecimal,
    lazyName: () -> Any
): BigDecimal {
    require(this < maximumExclusive) {
        "${lazyName()} must be less than $maximumExclusive, but was $this"
    }
    return this
}

public fun requireLessThan(
    value: BigDecimal,
    maximumExclusive: BigDecimal,
    name: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value < maximumExclusive) {
        "$name must be less than $maximumExclusive, but was $value"
    }
    return value
}

public fun requireLessThan(
    value: BigDecimal,
    name: String,
    other: BigDecimal,
    otherName: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value < other) {
        "$name must be less than $otherName ($other), but was $value"
    }
    return value
}

public inline fun Int.requireAtMost(
    maximumInclusive: Int,
    lazyName: () -> Any
): Int {
    require(this <= maximumInclusive) {
        "${lazyName()} must be at most $maximumInclusive, but was $this"
    }
    return this
}

public fun requireAtMost(
    value: Int,
    maximumInclusive: Int,
    name: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value <= maximumInclusive) {
        "$name must be at most $maximumInclusive, but was $value"
    }
    return value
}

public fun requireAtMost(
    value: Int,
    name: String,
    other: Int,
    otherName: String
): Int {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value <= other) {
        "$name must be at most $otherName ($other), but was $value"
    }
    return value
}

public inline fun Long.requireAtMost(
    maximumInclusive: Long,
    lazyName: () -> Any
): Long {
    require(this <= maximumInclusive) {
        "${lazyName()} must be at most $maximumInclusive, but was $this"
    }
    return this
}

public fun requireAtMost(
    value: Long,
    maximumInclusive: Long,
    name: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value <= maximumInclusive) {
        "$name must be at most $maximumInclusive, but was $value"
    }
    return value
}

public fun requireAtMost(
    value: Long,
    name: String,
    other: Long,
    otherName: String
): Long {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value <= other) {
        "$name must be at most $otherName ($other), but was $value"
    }
    return value
}

public inline fun Double.requireAtMost(
    maximumInclusive: Double,
    lazyName: () -> Any
): Double {
    require(java.lang.Double.isFinite(this)) {
        "${lazyName()} must be finite, but was $this"
    }
    require(java.lang.Double.isFinite(maximumInclusive)) {
        "${lazyName()} maximumInclusive must be finite, but was $maximumInclusive"
    }
    require(this <= maximumInclusive) {
        "${lazyName()} must be at most $maximumInclusive, but was $this"
    }
    return this
}

public fun requireAtMost(
    value: Double,
    maximumInclusive: Double,
    name: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(maximumInclusive)) {
        "$name maximumInclusive must be finite, but was $maximumInclusive"
    }
    require(value <= maximumInclusive) {
        "$name must be at most $maximumInclusive, but was $value"
    }
    return value
}

public fun requireAtMost(
    value: Double,
    name: String,
    other: Double,
    otherName: String
): Double {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(java.lang.Double.isFinite(value)) {
        "$name must be finite, but was $value"
    }
    require(java.lang.Double.isFinite(other)) {
        "$otherName must be finite, but was $other"
    }
    require(value <= other) {
        "$name must be at most $otherName ($other), but was $value"
    }
    return value
}

public inline fun BigDecimal.requireAtMost(
    maximumInclusive: BigDecimal,
    lazyName: () -> Any
): BigDecimal {
    require(this <= maximumInclusive) {
        "${lazyName()} must be at most $maximumInclusive, but was $this"
    }
    return this
}

public fun requireAtMost(
    value: BigDecimal,
    maximumInclusive: BigDecimal,
    name: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(value <= maximumInclusive) {
        "$name must be at most $maximumInclusive, but was $value"
    }
    return value
}

public fun requireAtMost(
    value: BigDecimal,
    name: String,
    other: BigDecimal,
    otherName: String
): BigDecimal {
    require(name.isNotBlank()) {
        "name must not be blank"
    }
    require(otherName.isNotBlank()) {
        "otherName must not be blank"
    }
    require(value <= other) {
        "$name must be at most $otherName ($other), but was $value"
    }
    return value
}
