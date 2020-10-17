package org.oreon.core.context

import org.oreon.core.math.Vec3f
import org.oreon.core.math.Vec4f
import org.oreon.core.util.Constants
import java.io.IOException
import java.util.*

class Config {
    // screen settings
    val frameWidth: Int
    val frameHeight: Int

    // window settings
    val displayTitle: String
    val windowWidth: Int
    val windowHeight: Int

    // glfw opengl vsync
    var glfwGLVSync = false

    // anitaliasing
    val multisampling_sampleCount: Int
    val fxaaEnabled: Boolean

    // shadows settings
    val shadowsEnable: Boolean
    val shadowMapResolution: Int
    val shadowsQuality: Int

    // post processing effects
    val ssaoEnabled: Boolean
    val bloomEnabled: Boolean
    val depthOfFieldBlurEnabled: Boolean
    val motionBlurEnabled: Boolean
    val lightScatteringEnabled: Boolean
    val lensFlareEnabled: Boolean

    // dynamic render settings
    val renderWireframe: Boolean
    val renderUnderwater: Boolean
    val renderReflection: Boolean
    val renderRefraction: Boolean
    var clipplane: Vec4f

    // Vulkan Validation
    var vkValidation = false

    // Atmosphere parameters
    var sunRadius = 0f
    lateinit var sunPosition: Vec3f
    lateinit var sunColor: Vec3f
    var sunIntensity = 0f
    var ambient = 0f
    var AtmosphericScatteringEnable = false
    var atmosphereBloomFactor = 0f
    lateinit var fogColor: Vec3f
    var horizonVerticalShift = 0f
    var sightRange = 0f

    // postprocessing parameters
    var lightscatteringSampleCount = 0
    var lightscatteringDecay = 0f
    var motionblurSampleCount = 0f
    var motionblurBlurfactor = 0
    var bloomKernels = 0
    var bloomSigma = 0

    init {
        val properties = Properties()
        try {
            val vInputStream = Config::class.java.classLoader.getResourceAsStream("oe-config.properties")
            properties.load(vInputStream)
            vInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        windowWidth = Integer.valueOf(properties.getProperty("window.width"))
        windowHeight = Integer.valueOf(properties.getProperty("window.height"))
        displayTitle = properties.getProperty("display.title")
        frameWidth = Integer.valueOf(properties.getProperty("frame.width"))
        frameHeight = Integer.valueOf(properties.getProperty("frame.height"))
        multisampling_sampleCount = Integer.valueOf(properties.getProperty("multisampling.sample.count"))
        fxaaEnabled = Integer.valueOf(properties.getProperty("fxaa.enable")) == 1
        shadowsEnable = Integer.valueOf(properties.getProperty("shadows.enable")) == 1
        shadowMapResolution = Integer.valueOf(properties.getProperty("shadows.map.resolution"))
        shadowsQuality = Integer.valueOf(properties.getProperty("shadows.quality"))
        bloomEnabled = Integer.valueOf(properties.getProperty("bloom.enable")) == 1
        ssaoEnabled = Integer.valueOf(properties.getProperty("ssao.enable")) == 1
        motionBlurEnabled = Integer.valueOf(properties.getProperty("motionBlur.enable")) == 1
        lightScatteringEnabled = Integer.valueOf(properties.getProperty("lightScattering.enable")) == 1
        depthOfFieldBlurEnabled = Integer.valueOf(properties.getProperty("depthOfFieldBlur.enable")) == 1
        lensFlareEnabled = Integer.valueOf(properties.getProperty("lensFlare.enable")) == 1
        if (properties.getProperty("validation.enable") != null) {
            vkValidation = Integer.valueOf(properties.getProperty("validation.enable")) == 1
        }
        if (properties.getProperty("glfw.vsync") != null) {
            glfwGLVSync = Integer.valueOf(properties.getProperty("glfw.vsync")) == 1
        }
        renderWireframe = false
        renderUnderwater = false
        renderReflection = false
        renderRefraction = false
        clipplane = Constants.ZEROPLANE
        try {
            val vInputStream = Config::class.java.classLoader.getResourceAsStream("atmosphere-config.properties")
            if (vInputStream != null) {
                properties.load(vInputStream)
                vInputStream.close()
                sunRadius = java.lang.Float.valueOf(properties.getProperty("sun.radius"))
                sunPosition = Vec3f(
                        java.lang.Float.valueOf(properties.getProperty("sun.position.x")),
                        java.lang.Float.valueOf(properties.getProperty("sun.position.y")),
                        java.lang.Float.valueOf(properties.getProperty("sun.position.z"))).normalize()
                sunColor = Vec3f(
                        java.lang.Float.valueOf(properties.getProperty("sun.color.r")),
                        java.lang.Float.valueOf(properties.getProperty("sun.color.g")),
                        java.lang.Float.valueOf(properties.getProperty("sun.color.b")))
                sunIntensity = java.lang.Float.valueOf(properties.getProperty("sun.intensity"))
                ambient = java.lang.Float.valueOf(properties.getProperty("ambient"))
                AtmosphericScatteringEnable = Integer.valueOf(properties.getProperty("atmosphere.scattering.enable")) == 1
                horizonVerticalShift = java.lang.Float.valueOf(properties.getProperty("horizon.verticalShift"))
                atmosphereBloomFactor = java.lang.Float.valueOf(properties.getProperty("atmosphere.bloom.factor"))
                sightRange = java.lang.Float.valueOf(properties.getProperty("sightRange"))
                fogColor = Vec3f(java.lang.Float.valueOf(properties.getProperty("fog.color.r")),
                        java.lang.Float.valueOf(properties.getProperty("fog.color.g")),
                        java.lang.Float.valueOf(properties.getProperty("fog.color.b")))
                val fogBrightness = java.lang.Float.valueOf(properties.getProperty("fog.brightness"))
                fogColor = fogColor!!.mul(fogBrightness)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            val vInputStream = Config::class.java.classLoader.getResourceAsStream("postprocessing-config.properties")
            if (vInputStream != null) {
                properties.load(vInputStream)
                vInputStream.close()
                lightscatteringSampleCount = Integer.valueOf(properties.getProperty("lightscattering.samples.count"))
                lightscatteringDecay = java.lang.Float.valueOf(properties.getProperty("lightscattering.decay"))
                motionblurBlurfactor = Integer.valueOf(properties.getProperty("motionblur.blurfactor"))
                motionblurSampleCount = Integer.valueOf(properties.getProperty("motionblur.samples.count")).toFloat()
                bloomKernels = Integer.valueOf(properties.getProperty("bloom.kernels"))
                bloomSigma = Integer.valueOf(properties.getProperty("bloom.sigma"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}