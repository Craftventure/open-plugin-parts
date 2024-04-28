package net.craftventure.audioserver.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Resource(
    var type: String?,
    var location: String,
    val artist: String?,
    val cover: String?,
    val name: String?,
    @Json(name = "spotify_uri")
    val spotifyUri: String?,
    @Json(name = "listen_url")
    val listenUrl: String?
)
