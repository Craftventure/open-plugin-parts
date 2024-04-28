package net.craftventure.extension

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder

fun ByteArray.encodeAsBase64ToString() = Base64Coder.encode(this)?.joinToString("")
fun String.decodeBase64ToByteArray() = Base64Coder.decode(this)