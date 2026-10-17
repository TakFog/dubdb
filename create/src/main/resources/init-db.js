// init-db.js - Script di configurazione database MongoDB per DubDB

const dbName = 'dubdb'; // Sostituisci con il nome del tuo database se diverso
const targetDb = db.getSiblingDB(dbName);

console.log(`Inizializzazione database '${dbName}'...`);

// -------------------------------------------------------------
// 1. Collezione: movies
// -------------------------------------------------------------
targetDb.createCollection('movies');
targetDb.movies.createIndex({ "ids.IMDB": 1 }, { sparse: true });
targetDb.movies.createIndex({ "ids.TRAKT": 1 }, { sparse: true });
targetDb.movies.createIndex({ "ids.WIKIDATA": 1 }, { sparse: true });
targetDb.movies.createIndex({ "ids.WIKI": 1 }, { sparse: true });

// -------------------------------------------------------------
// 2. Collezione: actors
// -------------------------------------------------------------
targetDb.createCollection('actors');
targetDb.actors.createIndex({ "ids.IMDB": 1 }, { sparse: true });
targetDb.actors.createIndex({ "ids.TRAKT": 1 }, { sparse: true });
targetDb.actors.createIndex({ "ids.WIKIDATA": 1 }, { sparse: true });
targetDb.actors.createIndex({ "ids.WIKI": 1 }, { sparse: true });

// -------------------------------------------------------------
// 3. Collezione: dubbers
// -------------------------------------------------------------
targetDb.createCollection('dubbers');
targetDb.dubbers.createIndex({ "ids.IMDB": 1 }, { sparse: true });
targetDb.dubbers.createIndex({ "ids.TRAKT": 1 }, { sparse: true });
targetDb.dubbers.createIndex({ "ids.WIKIDATA": 1 }, { sparse: true });
targetDb.dubbers.createIndex({ "ids.WIKI": 1 }, { sparse: true });

// Indice fondamentale per MongoDubberRepository.findMostRecent()
targetDb.dubbers.createIndex({ "lastUpdate": -1 });

// -------------------------------------------------------------
// 4. Collezione: dubbedentities
// -------------------------------------------------------------
targetDb.createCollection('dubbedentities');

// Indici per query MongoDubbedEntityRepository.findByRef(DubberRef)
targetDb.dubbedentities.createIndex({ "dubber.ids.DUBDB": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "dubber.ids.WIKI": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "dubber.ids.IMDB": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "dubber.ids.TRAKT": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "dubber.ids.WIKIDATA": 1 }, { sparse: true });

// Indici per query MongoDubbedEntityRepository.findByRef(MovieRef)
targetDb.dubbedentities.createIndex({ "movie.ids.DUBDB": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "movie.ids.WIKI": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "movie.ids.IMDB": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "movie.ids.TRAKT": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "movie.ids.WIKIDATA": 1 }, { sparse: true });

// Indici per ricerca su referenza actor
targetDb.dubbedentities.createIndex({ "actor.ids.DUBDB": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "actor.ids.WIKI": 1 }, { sparse: true });
targetDb.dubbedentities.createIndex({ "actor.ids.IMDB": 1 }, { sparse: true });
