package takutility.dubdb.db

import takutility.dubdb.entities.Movie
import takutility.dubdb.entities.RawData
import takutility.dubdb.entities.SourceIds

internal class MemMovieRepositoryTest: RepositoryTest<Movie>() {
    override fun newRepo(): EntityRepository<Movie> = MemMovieRepository()

    override fun newEntity(name: String, ids: SourceIds, parsed: Boolean, sources: MutableList<RawData>) = Movie(
        name = name, ids = ids, parsed = parsed, sources = sources
    )
}