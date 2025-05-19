package takutility.dubdb.util

fun <T: Comparable<T>> minOrNull(vararg a: Collection<T?>): T? = a.asSequence().flatMap { it }.reduce(::minOrNull)

fun <T: Comparable<T>> minOrNull(a: T?, b: T?): T? = if (a == null || b == null) null else minOf(a, b)