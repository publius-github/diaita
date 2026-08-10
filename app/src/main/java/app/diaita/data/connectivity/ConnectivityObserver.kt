package app.diaita.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

class ConnectivityObserver(
    context: Context,
    scope: CoroutineScope,
) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Shared eagerly on [scope] so exactly one system [ConnectivityManager.NetworkCallback]
     * lives for the whole process — not re-registered on every search debounce tick, which
     * is what a bare cold flow did here before. Eager start also keeps `first()`/`value`
     * truthful from the very first read (a lazy start would answer with a stale initial).
     */
    val isOnline: StateFlow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(hasInternet())
                    }

                    override fun onLost(network: Network) {
                        trySend(hasInternet())
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        trySend(hasInternet())
                    }
                }
            val request =
                NetworkRequest
                    .Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            connectivityManager.registerNetworkCallback(request, callback)
            trySend(hasInternet())
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = hasInternet(),
        )

    private fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
