package takutility.dubdb

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource

data class TraktConfig(val client_id: String)

data class MongoDBConfig(val url: String, val db: String)

data class WikiConfig(val cache: String?, val categoryLimit : Int = 500)

data class BootstrapConfig(val movie: String, val dub: String)

data class Config(
        val trakt: TraktConfig,
        val mongodb: MongoDBConfig,
        val wiki: WikiConfig = WikiConfig(null),
        val bootstrap: BootstrapConfig,
        val retry: Int = 3)

fun loadConfig() = ConfigLoaderBuilder.default()
        .addResourceOrFileSource("config_local.yml", optional = true)
        .addResourceOrFileSource("config.yml")
        .build()
        .loadConfigOrThrow<Config>()