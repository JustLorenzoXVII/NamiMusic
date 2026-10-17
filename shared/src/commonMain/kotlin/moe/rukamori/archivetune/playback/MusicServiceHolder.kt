package moe.rukamori.archivetune.playback

/**
 * Il MusicService attivo. Nell'originale era un servizio Android a cui ci si collegava con un Binder;
 * qui è un oggetto nel processo, creato all'avvio dell'interfaccia.
 */
object MusicServiceHolder {
    var instance: MusicService? = null
}
