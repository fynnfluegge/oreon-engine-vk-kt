package org.oreon.common.water

import org.oreon.core.math.Vec2f
import org.oreon.core.math.Vec3f
import java.io.IOException
import java.lang.Boolean
import java.util.*

class WaterConfig {
    var n = 0
    var l = 0
    var amplitude = 0f
    lateinit var windDirection: Vec2f
    var windSpeed = 0f
    var alignment = 0f
    var capillarWavesSupression = 0f
    var displacementScale = 0f
    var choppiness = 0f
    var tessellationFactor = 0
    var tessellationShift = 0f
    var tessellationSlope = 0f
    var highDetailRange = 0
    var uvScale = 0
    var specularFactor = 0f
    var specularAmplifier = 0f
    var diffuse = false
    var emission = 0f
    var kReflection = 0f
    var kRefraction = 0f
    var distortion = 0f
    var fresnelFactor = 0f
    var waveMotion = 0f
    var normalStrength = 0f
    var t_delta = 0f
    var choppy = false
    lateinit var baseColor: Vec3f
    var reflectionBlendFactor = 0f
    var capillarStrength = 0f
    var capillarDownsampling = 0f
    var dudvDownsampling = 0f
    var underwaterBlur = 0f
    fun loadFile(file: String?) {
        val properties = Properties()
        try {
            val stream = WaterConfig::class.java.classLoader.getResourceAsStream(file)
            properties.load(stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        n = Integer.valueOf(properties.getProperty("fft.resolution"))
        l = Integer.valueOf(properties.getProperty("fft.L"))
        amplitude = java.lang.Float.valueOf(properties.getProperty("fft.amplitude"))
        windDirection = Vec2f(java.lang.Float.valueOf(properties.getProperty("wind.x")),
                java.lang.Float.valueOf(properties.getProperty("wind.y"))).normalize()
        windSpeed = java.lang.Float.valueOf(properties.getProperty("wind.speed"))
        alignment = java.lang.Float.valueOf(properties.getProperty("alignment"))
        capillarWavesSupression = java.lang.Float.valueOf(properties.getProperty("fft.capillarwavesSuppression"))
        displacementScale = java.lang.Float.valueOf(properties.getProperty("displacementScale"))
        choppiness = java.lang.Float.valueOf(properties.getProperty("choppiness"))
        distortion = java.lang.Float.valueOf(properties.getProperty("distortion_delta"))
        waveMotion = java.lang.Float.valueOf(properties.getProperty("wavemotion"))
        uvScale = Integer.valueOf(properties.getProperty("uvScale"))
        tessellationFactor = Integer.valueOf(properties.getProperty("tessellationFactor"))
        tessellationSlope = java.lang.Float.valueOf(properties.getProperty("tessellationSlope"))
        tessellationShift = java.lang.Float.valueOf(properties.getProperty("tessellationShift"))
        specularFactor = java.lang.Float.valueOf(properties.getProperty("specular.factor"))
        specularAmplifier = java.lang.Float.valueOf(properties.getProperty("specular.amplifier"))
        emission = java.lang.Float.valueOf(properties.getProperty("emission.factor"))
        kReflection = java.lang.Float.valueOf(properties.getProperty("kReflection"))
        kRefraction = java.lang.Float.valueOf(properties.getProperty("kRefraction"))
        normalStrength = java.lang.Float.valueOf(properties.getProperty("normalStrength"))
        highDetailRange = Integer.valueOf(properties.getProperty("highDetailRange"))
        t_delta = java.lang.Float.valueOf(properties.getProperty("t_delta"))
        choppy = Boolean.valueOf(properties.getProperty("choppy"))
        fresnelFactor = java.lang.Float.valueOf(properties.getProperty("fresnel.factor"))
        reflectionBlendFactor = java.lang.Float.valueOf(properties.getProperty("reflection.blendfactor"))
        baseColor = Vec3f(java.lang.Float.valueOf(properties.getProperty("water.basecolor.x")),
                java.lang.Float.valueOf(properties.getProperty("water.basecolor.y")),
                java.lang.Float.valueOf(properties.getProperty("water.basecolor.z")))
        capillarStrength = java.lang.Float.valueOf(properties.getProperty("capillar.strength"))
        capillarDownsampling = java.lang.Float.valueOf(properties.getProperty("capillar.downsampling"))
        dudvDownsampling = java.lang.Float.valueOf(properties.getProperty("dudv.downsampling"))
        underwaterBlur = java.lang.Float.valueOf(properties.getProperty("underwater.blurfactor"))
        diffuse = if (Integer.valueOf(properties.getProperty("diffuse.enable")) == 0) false else true
    }
}