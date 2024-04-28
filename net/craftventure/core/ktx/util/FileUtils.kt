package net.craftventure.core.ktx.util

import java.io.File

fun File.getRelativeFileName(withExtension: Boolean = true, root: File?): String {
    val file = if (root != null) this.relativeTo(root) else this
    val path = file.toPath()

    var name = ""
    for (i in 0 until path.nameCount - 1) {
        if (name.isNotEmpty())
            name += "/"
        name += path.getName(i)
    }
    if (name.isNotEmpty())
        name += "/"
    name += if (withExtension) file.name else file.nameWithoutExtension
    return name
}