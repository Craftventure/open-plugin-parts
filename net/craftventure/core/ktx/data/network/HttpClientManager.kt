package net.craftventure.core.ktx.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientManager {
    fun createClient(): OkHttpClient {
//        val logging = HttpLoggingInterceptor()
//        if (PluginProvider.isTestServer()) logging.level = HttpLoggingInterceptor.Level.NONE
        return OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
//            .addInterceptor(logging)
            .build()
    }

    @JvmStatic
    val okhttpClient by lazy { createClient() }
}