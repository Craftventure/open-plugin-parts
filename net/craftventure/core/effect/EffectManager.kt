package net.craftventure.core.effect


object EffectManager {
    private val simpleEffects = HashMap<String, SimpleEffect>()

    fun add(simpleEffect: SimpleEffect) {
        simpleEffects[simpleEffect.name] = simpleEffect
    }

    fun findByName(name: String): SimpleEffect? {
        return simpleEffects[name]
    }

    fun keys() = simpleEffects.keys

    fun shutdown() {
        for (simpleEffect in simpleEffects.values) {
            simpleEffect.stop()
        }
    }

    init {
        add(MatShopExitDoor())
    }
}
