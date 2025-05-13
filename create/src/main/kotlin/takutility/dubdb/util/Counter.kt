package takutility.dubdb.util


fun <T> mutableCounter(): MutableCounter<T> {
    return MutableCounterImpl()
}

interface Counter<T> {
    // Query Operations
    /**
     * Returns the number of values in the counter.
     */
    val size: Int

    /**
     * Returns `true` if the counter is empty (contains no elements), `false` otherwise.
     */
    fun isEmpty(): Boolean

    /**
     * Returns `true` if the map contains the specified [value].
     */
    operator fun contains(value: T): Boolean

    /**
     * Returns the value corresponding to the given [value], or `0` if such a value is not present in the counter.
     */
    operator fun get(value: T): Int

    operator fun plus(other: Counter<T>): Counter<T>

    fun forEach(action: (T, Int) -> Unit)
    
    fun immutable(): Counter<T>

    fun mutable(): MutableCounter<T>

    // Views
    /**
     * Returns a read-only [Set] of all values in this map.
     */
    val values: Set<T>

    /**
     * Returns a read-only [Set] of all value/count pairs in this counter.
     */
    val pairs: Set<Map.Entry<T, Int>>
}

interface MutableCounter<T> : Counter<T> {
    // Modification Operations
    /**
     * Increments the count of the specified [value].
     */
    fun add(value: T) = add(value, 1)

    /**
     * Increments the count of the specified [value] by [count].
     */
    fun add(value: T, count: Int)

    /**
     * Associates the specified [value] in the counter.
     */
    fun set(value: T, count: Int)
    
    operator fun plusAssign(other: Counter<T>)

    /**
     * Removes the specified value from this counter.
     */
    fun remove(value: T)

    // Bulk Modification Operations
    /**
     * Updates this counter with values from the specified iterable [from].
     */
    fun addAll(from: Iterable<T>)

    /**
     * Updates this counter with values from the specified sequence [from].
     */
    fun addAll(from: Sequence<T>)

    /**
     * Insert in the counter all the missing values from the iterable [from]
     */
    fun setDefaults(from: Iterable<T>)

    /**
     * Removes all elements from this map.
     */
    fun clear()

    // Views
    /**
     * Returns a [MutableSet] of all values in this counter.
     */
    override val values: MutableSet<T>

    /**
     * Returns a [MutableSet] of all value/count pairs in this counter.
     */
    override val pairs: MutableSet<MutableMap.MutableEntry<T, Int>>
}

private open class CounterImpl<T>(protected open val map: Map<T, Int> = mapOf()) : Counter<T> {

    override val size: Int
        get() = map.size

    override fun isEmpty() = map.isEmpty()

    override fun forEach(action: (T, Int) -> Unit) = map.forEach(action)

    override fun plus(other: Counter<T>): Counter<T> {
        val mut = this.mutable()
        mut += other
        return mut.immutable()
    }

    override fun immutable() = this

    override fun mutable(): MutableCounter<T> = MutableCounterImpl(map.toMutableMap())

    override fun get(value: T) = map.getOrDefault(value, 0)

    override fun contains(value: T) = map.containsKey(value)

    override val values: Set<T>
        get() = map.keys
    override val pairs: Set<Map.Entry<T, Int>>
        get() = map.entries

}

private class MutableCounterImpl<T>(override val map: MutableMap<T, Int> = mutableMapOf()) : CounterImpl<T>(), MutableCounter<T> {

    override fun add(value: T, count: Int) {
        map.compute(value) {_,v -> (v ?: 0) + count}
    }

    override fun set(value: T, count: Int) {
        map[value] = count
    }

    override fun remove(value: T) {
        map.remove(value)
    }

    override fun addAll(from: Iterable<T>) {
        from.forEach(this::add)
    }

    override fun addAll(from: Sequence<T>) {
        from.forEach(this::add)
    }

    override fun setDefaults(from: Iterable<T>) {
        from.forEach { map.computeIfAbsent(it) { 0 } }
    }

    override fun plusAssign(other: Counter<T>) {
        other.forEach { v,c -> add(v, c) }
    }

    override fun clear() = map.clear()

    override fun immutable(): CounterImpl<T> {
        return CounterImpl(map.toMap())
    }

    override val values: MutableSet<T>
        get() = map.keys
    override val pairs: MutableSet<MutableMap.MutableEntry<T, Int>>
        get() = map.entries

}