package net.craftventure.chat.bungee.extension

import com.linkedin.urls.Url
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions

fun String.containingUrls(): MutableList<Url> = UrlDetector(this, UrlDetectorOptions.Default).detect()