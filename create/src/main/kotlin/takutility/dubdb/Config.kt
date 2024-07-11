package takutility.dubdb

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource

data class TraktConfig(val client_id: String)

data class MongoDBConfig(val url: String, val db: String)

data class WikiConfig(val cache: String?)

data class BootstrapConfig(val movie: String, val dub: String)

data class Config(
        val trakt: TraktConfig,
        val mongodb: MongoDBConfig,
        val wiki: WikiConfig?,
        val bootstrap: BootstrapConfig,
        val retry: Int = 3) {

    companion object {
        val inst: Config = loadConfig()
    }
}

fun loadConfig() = ConfigLoaderBuilder.default()
        .addResourceOrFileSource("config_local.yml", optional = true)
        .addResourceOrFileSource("config.yml")
        .build()
        .loadConfigOrThrow<Config>()