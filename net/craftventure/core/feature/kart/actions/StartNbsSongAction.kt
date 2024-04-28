package net.craftventure.core.feature.kart.actions

import net.craftventure.bukkit.ktx.nbs.NbsPlayer
import net.craftventure.core.extension.openMenu
import net.craftventure.core.feature.instrument.InstrumentType
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import net.craftventure.core.feature.nbsplayback.NbsFileManager
import net.craftventure.core.inventory.impl.OwnedItemsMenu
import net.craftventure.core.inventory.impl.OwnedItemsPickerMenu
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.database.type.ItemType
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

class StartNbsSongAction(
    val instrument: InstrumentType,
) : KartAction {
    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
        kart.metadata["nbssong"]?.onDestroy()

        val pickerMenu = OwnedItemsPickerMenu(
            kart.player,
            object : OwnedItemsMenu.ItemFilter {
                override val displayName: String = ItemType.MUSIC_SHEET.displayNamePlural

                override fun matches(itemType: ItemType): Boolean = itemType == ItemType.MUSIC_SHEET

                override fun matches(ownableItem: OwnableItem): Boolean {
                    val supported = ownableItem.getOwnableItemMetadata()?.musicSheet?.instruments ?: return false
                    return instrument in supported
                }
            }
        ) { pickedItem ->
            if (!kart.isValid()) return@OwnedItemsPickerMenu
            val songName = pickedItem.getOwnableItemMetadata()?.musicSheet?.song ?: return@OwnedItemsPickerMenu
            val song = NbsFileManager.getSong(songName) ?: return@OwnedItemsPickerMenu

            val player = NbsPlayer(song) { block, sound, volume, pitch ->
                kart.world.playSound(
                    kart.location.toLocation(kart.world),
                    sound,
                    SoundCategory.BLOCKS,
                    volume,
                    pitch
                )
            }
            player.start()
            kart.metadata["nbssong"] = object : Kart.Attachment {
                override fun onDestroy() {
                    player.stop()
                }
            }
        }
        kart.player.openMenu(pickerMenu)
    }
}
