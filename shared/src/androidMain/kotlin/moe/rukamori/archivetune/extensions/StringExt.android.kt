package moe.rukamori.archivetune.extensions

import java.net.InetSocketAddress
import java.net.InetSocketAddress.createUnresolved

fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return createUnresolved(host, port.toInt())
}
