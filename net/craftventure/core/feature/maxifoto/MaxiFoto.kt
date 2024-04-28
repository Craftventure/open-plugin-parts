package net.craftventure.core.feature.maxifoto

import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.packet.PacketParkPhoto
import net.craftventure.bukkit.ktx.extension.getTextureDataBase64
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.api.MaxiFotoResponse
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.json.parseJson
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.TracerUtils
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.MapEntry
import okhttp3.ResponseBody
import okio.ByteString.Companion.toByteString
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.UnknownHostException
import java.util.*


object MaxiFoto {
    fun cacheSkin(player: Player) {
        CvApi.maxifotoService.cache(player.uniqueId.toString(), player.playerProfile.getTextureDataBase64())
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    //                Logger.console("Call " + call.request().url() + " completed");
                }

                override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                    throwable.printStackTrace()
                    //                Logger.capture(throwable);
                }
            })
    }

    private fun render(renderSettings: RenderSettings, renderListener: RenderListener?) {
        if (renderListener != null) {
//            Logger.debug("Players=${renderSettings.players.joinToString(", ") { it?.name ?: "null" }}")
//            Logger.debug("UUIDs=${renderSettings.players.map { it?.uniqueId?.toString() }}")
//            Logger.debug("Textures=${renderSettings.players.map { it?.getGameProfile()?.getTextureDataBase64() }}")
            CvApi.maxifotoService
                .render(
                    renderSettings.ride,
                    renderSettings.players.map { it?.uniqueId?.toString() ?: "" },
                    renderSettings.players.map { it?.playerProfile?.getTextureDataBase64() ?: "" }
                )
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        //                    Logger.console("Call " + call.request().url() + " completed");
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            try {
                                //                            Logger.console("Body " + body);
                                if (body != null) {
                                    val maxiFotoResponse = parseJson<MaxiFotoResponse>(body)
                                    if (maxiFotoResponse != null) {
                                        renderListener.onRenderCompleted(maxiFotoResponse)
                                    } else {
                                        Logger.severe("Maxifoto body = null")
                                    }
                                } else {
                                    Logger.severe("No maxifoto body received")
                                }
                                return
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Logger.severe("Failed JSON body$body")
                                renderListener.onRenderFailed(e)
                                return
                            }
                        }
                        renderListener.onRenderFailed(IllegalStateException("Response not successful"))
                    }

                    override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                        if (TracerUtils.getRootCause(throwable) !is UnknownHostException) {
                            Logger.capture(IllegalStateException("Maxifoto failed", throwable))
                        }
                        renderListener.onRenderFailed(throwable)
                    }
                })
        }
    }

    fun render(renderSettings: RenderSettings) {
        render(renderSettings, object : RenderListener {
            override fun onRenderCompleted(maxiFotoResponse: MaxiFotoResponse) {
                if (!maxiFotoResponse.isSuccess) {
                    Logger.info("Failed to render maxifoto ${renderSettings.ride} (no success)")
                    return
                }
                if (maxiFotoResponse.renderId == null) {
                    Logger.info("Failed to render maxifoto ${renderSettings.ride} (renderId = null)")
                    return
                }
                executeAsync {
                    val mapEntryList: MutableList<MapEntry> = ArrayList()
                    for (picture in maxiFotoResponse.pictures) {
                        try {
                            val response = CvApi.maxifotoService.retrieveMaxiFoto(picture.name).execute()
                            if (response.isSuccessful) {
                                val body = response.body()
                                val bytes = body!!.bytes()

                                val fos = FileOutputStream(
                                    File(
                                        CraftventureCore.getInstance().dataFolder,
                                        "data/maps/maxifoto/" + maxiFotoResponse.renderId + "_" + (picture.pictureId + renderSettings.offset) + ".png"
                                    )
                                )
                                fos.write(bytes)
                                fos.close()

                                if (picture.players != null && picture.players.isNotEmpty()) {
                                    //                                        Logger.console("Retrieved %d players", picture.getPlayers().size());
                                    val base64Image = bytes.toByteString(0, bytes.size).base64()
                                    val photoPersonList = ArrayList<PacketParkPhoto.PhotoPerson>()
                                    for (uuid in picture.players) {
                                        //                                            Logger.console("Player %s", name);
                                        photoPersonList.add(PacketParkPhoto.PhotoPerson(null, uuid))
                                    }

                                    val packetParkPhoto = PacketParkPhoto(
                                        base64Image, photoPersonList, maxiFotoResponse.renderId,
                                        System.currentTimeMillis(), PacketParkPhoto.Type.ONRIDE_PICTURE
                                    )
                                    for (uuid in picture.players) {
                                        val player = Bukkit.getPlayer(UUID.fromString(uuid))
                                        //                                            Logger.console("Checking %s", name);
                                        if (player != null) {
                                            //                                                Logger.console("Sending park photo to %s", player.getName());
                                            if (!AudioServer.instance.audioServer!!.sendPacket(
                                                    player,
                                                    packetParkPhoto
                                                )
                                            ) {
                                                player.sendMessage(
                                                    Translation.PARK_PICTURE_TAKEN.getTranslation(
                                                        player
                                                    )!!
                                                )
                                            } else {
                                                player.sendMessage(CVTextColor.serverNotice + "A ride picture was sent to the AudioServer feed")
                                            }
                                        }
                                    }
                                }
                            } else {
                                Logger.severe("Download failed")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val id = picture.pictureId + renderSettings.offset
                        val mapEntries = MainRepositoryProvider.mapEntriesRepository
                            .getByTrigger(maxiFotoResponse.renderId + "_" + id)
                        //                            MapManager.getInstance().invalidateRide(renderSettings.ride);
                        mapEntryList.addAll(mapEntries)
                        //                            } else {
                        if (mapEntries.isEmpty())
                            Logger.severe("MaxiFoto tried rendering " + maxiFotoResponse.renderId + "_" + id + ", but does not exist in database!")
                        //                            }
                    }

                    executeSync {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
                            for (i in mapEntryList.indices) {
                                val mapEntry = mapEntryList[i]
                                MapManager.instance.updateMap(mapEntry)
                            }
                        }
                    }
                }
            }

            override fun onRenderFailed(throwable: Throwable) {
                Logger.severe("Failed to render maxifoto ${renderSettings.ride}: ${throwable.message}")
            }
        })
    }

    class RenderSettings(val ride: String, val players: Array<Player?>) {
        var offset = 0
    }

    interface RenderListener {
        fun onRenderCompleted(maxiFotoResponse: MaxiFotoResponse)

        fun onRenderFailed(throwable: Throwable)
    }
}
