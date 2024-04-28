package net.craftventure.database

object MainRepositoryProvider : RepositoryProvider by MainRepositoryHolder.provider
object MainRepositoryHolder {
    lateinit var provider: RepositoryProvider
}