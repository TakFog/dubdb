package takutility.dubdb.util

fun <T> Sequence<T>.countInstances(initialValues: Iterable<T>? = null): Map<T, Int> {
    val counts = initialValues?.associateWithTo(mutableMapOf()) { 0 } ?: mutableMapOf()

    this.forEach { item ->
        counts[item] = counts.getOrDefault(item, 0) + 1
    }
    return counts
}

fun <T> Iterable<T>.countInstances(initialValues: Iterable<T>? = null) = this.asSequence().countInstances(initialValues)
