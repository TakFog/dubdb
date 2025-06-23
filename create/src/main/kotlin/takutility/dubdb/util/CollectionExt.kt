package takutility.dubdb.util

fun <T> Sequence<T>.countInstances(initialValues: Iterable<T>? = null): Counter<T> {
    val counts = mutableCounter<T>()
    counts.addAll(this)
    initialValues?.let { counts.setDefaults(initialValues) }
    return counts
}

fun <T> Iterable<T>.countInstances(initialValues: Iterable<T>? = null) = this.asSequence().countInstances(initialValues)

fun Collection<Any>.isDistinct() = this.toSet().size == this.size
fun <T, K> Collection<T>.isDistinct(selector: (T) -> K) = this.distinctBy(selector).size == this.size

fun <K, V> MutableMap<K, MutableList<V>>.append(key: K, value: V) = this.computeIfAbsent(key) { mutableListOf() }.add(value)

fun <E> List<E>.limit(size: Int) = if (this.size <= size) this else this.subList(0, size)