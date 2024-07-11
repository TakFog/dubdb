package takutility.dubdb.util

open class Cache<T>(private val size: Int, private val action: (List<T>) -> Unit, onFull: (() -> Unit)? = null) : AutoCloseable {
    private val list = mutableListOf<T>()
    private val onFull: () -> Unit

    init {
        this.onFull = onFull ?: this::flush
    }

    open fun add(element: T) {
        list.add(element)
        if (list.size >= size)
            onFull()
    }

    open operator fun plusAssign(elements: Iterable<T>) {
        list += elements
        if (list.size >= size)
            onFull()
    }

    open fun flush() {
        if (list.isNotEmpty()) {
            action(list)
            list.clear()
        }
    }

    override fun close() = flush()
}