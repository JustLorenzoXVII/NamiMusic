package moe.rukamori.archivetune

internal actual fun platformArchitecture(): String = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
