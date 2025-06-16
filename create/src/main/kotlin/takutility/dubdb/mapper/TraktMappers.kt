package takutility.dubdb.mapper

import takutility.dubdb.entities.MovieType
import takutility.dubdb.entities.Source
import takutility.dubdb.entities.SourceIds
import takutility.dubdb.entities.movieRefOf
import takutility.dubdb.service.MovieOrShow

fun MovieOrShow.toRef() = movieRefOf(
    title,
    type = if (isMovie()) MovieType.MOVIE else MovieType.SERIES,
    ids = SourceIds.of(Source.TRAKT to ids?.trakt?.toString(), Source.IMDB to ids?.imdb)
)