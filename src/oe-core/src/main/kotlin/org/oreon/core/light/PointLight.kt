package org.oreon.core.light

import org.oreon.core.math.Vec3f

class PointLight(var position: Vec3f, color: Vec3f, intensity: Float) : Light(color, intensity) {
    var isEnabled = 0
    var isSpot = 0
    var constantAttenuation = 0f
    var linearAttenuation = 0f
    var quadraticAttenuation = 0f
    var coneDirection: Vec3f? = null
    var spotCosCutoff = 0f
    var spotExponent = 0f

}