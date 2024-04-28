package net.craftventure.core.script.timeline

import penner.easing.*


enum class KeyFrameEasing(val easing: (t: Double, b: Double, c: Double, d: Double) -> Double) {
    NONE(Linear::easeNone),
    PREVIOUS(Previous::easeNone),
    LINEAR(Linear::easeNone),

    BACK_EASE_IN(Back::easeIn),
    BACK_EASE_OUT(Back::easeOut),
    BACK_EASE_IN_OUT(Back::easeInOut),

    BOUNCE_EASE_IN(Bounce::easeIn),
    BOUNCE_EASE_OUT(Bounce::easeOut),
    BOUNCE_EASE_IN_OUT(Bounce::easeInOut),

    CIRC_EASE_IN(Circ::easeIn),
    CIRC_EASE_OUT(Circ::easeOut),
    CIRC_EASE_IN_OUT(Circ::easeInOut),

    CUBIC_EASE_IN(Cubic::easeIn),
    CUBIC_EASE_OUT(Cubic::easeOut),
    CUBIC_EASE_IN_OUT(Cubic::easeInOut),

    ELASTIC_EASE_IN(Elastic::easeIn),
    ELASTIC_EASE_OUT(Elastic::easeOut),
    ELASTIC_EASE_IN_OUT(Elastic::easeInOut),

    EXPO_EASE_IN(Expo::easeIn),
    EXPO_EASE_OUT(Expo::easeOut),
    EXPO_EASE_IN_OUT(Expo::easeInOut),

    LINEAR_EASE_IN(Linear::easeIn),
    LINEAR_EASE_OUT(Linear::easeOut),
    LINEAR_EASE_IN_OUT(Linear::easeInOut),

    QUAD_EASE_IN(Quad::easeIn),
    QUAD_EASE_OUT(Quad::easeOut),
    QUAD_EASE_IN_OUT(Quad::easeInOut),

    QUART_EASE_IN(Quart::easeIn),
    QUART_EASE_OUT(Quart::easeOut),
    QUART_EASE_IN_OUT(Quart::easeInOut),

    QUINT_EASE_IN(Quint::easeIn),
    QUINT_EASE_OUT(Quint::easeOut),
    QUINT_EASE_IN_OUT(Quint::easeInOut),

    SINE_EASE_IN(Sine::easeIn),
    SINE_EASE_OUT(Sine::easeOut),
    SINE_EASE_IN_OUT(Sine::easeInOut)
}