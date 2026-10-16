package moe.rukamori.archivetune.compat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Sostituto di androidx.lifecycle.ProcessLifecycleOwner: il ciclo di vita dell'intera app.
 * Lo stato viene aggiornato dalla piattaforma con [ProcessLifecycleOwner.onForegroundChanged]
 * (chiamare sempre sul thread principale).
 */
class ProcessLifecycleOwner private constructor() : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }

    override val lifecycle: Lifecycle
        get() = registry

    companion object {
        private val instance by lazy { ProcessLifecycleOwner() }

        fun get(): ProcessLifecycleOwner = instance

        fun onForegroundChanged(foreground: Boolean) {
            instance.registry.currentState = if (foreground) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
        }
    }
}
