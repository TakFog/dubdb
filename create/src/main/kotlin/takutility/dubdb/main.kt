package takutility.dubdb

import mu.KotlinLogging
import takutility.dubdb.ops.dubber.Dubbers
import takutility.dubdb.ops.movie.Movies

private val logger = KotlinLogging.logger {}

class Args(args: Array<String>) {
    val iterations: Int
    val startFromMovies: Boolean = !args.contains("--dubbers")

    init {
        var iterValue: Int? = null
        val iterIndex = args.indexOfFirst { it.equals("-i") || it.equals("--iterations") }
        if (iterIndex >= 0)
            iterValue = args.getOrNull(iterIndex + 1)?.toInt()
        iterations = iterValue ?: Int.MAX_VALUE
    }
}

fun main(args: Array<String>) {
    logger.info { "Starting DubDB data extraction" }

    val config = loadConfig()
    val context = contextFromConfig(config)

    val pargs = Args(args)
    for (i in 1..pargs.iterations) {
        logger.info { "Iteration $i" }
        if (i > 1 || pargs.startFromMovies)
            context[Movies::class].tryRun { it.run(20) }
        context[Dubbers::class].tryRun { it.run(20) }
        flush?.run()
    }
}

private inline fun <reified T : Any> T.tryRun(action: (T) -> Unit) {
    try {
        action(this)
    } catch (e: Exception) {
        logger.error(e) { "Error while running ${T::class.qualifiedName} code" }
    }
}