package takutility.dubdb.util

class KeyCache<K, T>(size: Int, private val keyExtractor: (T) -> K, action: (List<T>) -> Unit, onFull: (() -> Unit)?) : Cache<T>(size, action, onFull) {
    private val map = mutableMapOf<K,T>()

    operator fun get(key: K) = map[key]
    @JvmName("getElement")
    operator fun get(e: T) = map[keyExtractor(e)]

    operator fun contains(key: K) = key in map
    @JvmName("containsElement")
    operator fun contains(e: T) = keyExtractor(e) in map

    override fun add(element: T) {
        val k = keyExtractor(element)
        map[k] = element
        super.add(element)
    }

    override fun plusAssign(elements: Iterable<T>) {
        elements.forEach { e -> map[keyExtractor(e)] = e }
        super.plusAssign(elements)
    }

    override fun flush() {
        super.flush()
        map.clear()
    }
}