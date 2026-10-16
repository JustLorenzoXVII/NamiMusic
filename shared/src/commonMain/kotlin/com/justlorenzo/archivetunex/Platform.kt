package com.justlorenzo.archivetunex

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform