package takutility.dubdb.util

import takutility.dubdb.entities.ActorRef
import takutility.dubdb.entities.DubberRef
import takutility.dubdb.entities.EntityRef
import takutility.dubdb.entities.MovieRef

fun <T: Comparable<T>> minOrNull(vararg a: Collection<T?>): T? = a.asSequence().flatMap { it }.reduce(::minOrNull)

fun <T: Comparable<T>> minOrNull(a: T?, b: T?): T? = if (a == null || b == null) null else minOf(a, b)

fun <E>  List<E>.notEmpty() = this.ifEmpty { null }

fun Collection<EntityRef>.splitByType(actors: MutableList<ActorRef>,
                                      dubbers: MutableList<DubberRef>,
                                      movies: MutableList<MovieRef>)
{
    this.forEach {
        when(it) {
            is ActorRef -> actors.add(it)
            is DubberRef -> dubbers.add(it)
            is MovieRef -> movies.add(it)
        }
    }
}