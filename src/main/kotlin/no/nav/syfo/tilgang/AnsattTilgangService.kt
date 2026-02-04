package no.nav.syfo.tilgang

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AnsattTilgangService(
    private val narmestelederClient: NarmestelederClient
) {
    private val ansatteCache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(Duration.ofMinutes(CACHE_TTL_MINUTES))
        .build()

    private val inFlight = ConcurrentHashMap<String, Mutex>()

    suspend fun hasAccessToAnsatt(
        innloggetFnr: String,
        ansattFnr: String,
        callId: String?,
        consumerId: String?
    ): Boolean {
        val cached = ansatteCache.getIfPresent(innloggetFnr)
        if (cached != null) {
            return cached.contains(ansattFnr)
        }

        // Avoid multiple concurrent fetches per leader when cache is cold.
        val mutex = inFlight.computeIfAbsent(innloggetFnr) { Mutex() }
        return mutex.withLock {
            try {
                val secondCheck = ansatteCache.getIfPresent(innloggetFnr)
                if (secondCheck != null) {
                    return@withLock secondCheck.contains(ansattFnr)
                }

                val ansatte = narmestelederClient
                    .ansatte(innloggetFnr, callId, consumerId)
                    .asSequence()
                    .map { it.fnr }
                    .toSet()

                ansatteCache.put(innloggetFnr, ansatte)
                ansatte.contains(ansattFnr)
            } finally {
                // Best-effort cleanup to avoid unbounded growth of the in-flight map.
                inFlight.remove(innloggetFnr, mutex)
            }
        }
    }

    companion object {
        private const val CACHE_TTL_MINUTES = 5L
        private const val CACHE_MAX_SIZE = 10_000L
    }
}
