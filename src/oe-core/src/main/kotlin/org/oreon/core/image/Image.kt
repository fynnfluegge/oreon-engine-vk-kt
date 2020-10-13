package org.oreon.core.image

open class Image {
    var metaData: ImageMetaData? = null
        protected set

    enum class ImageFormat {
        RGBA8_SNORM, RGBA32FLOAT, RGB32FLOAT, RGBA16FLOAT, DEPTH32FLOAT, R16FLOAT, R32FLOAT, R8
    }

    enum class SamplerFilter {
        Nearest, Bilinear, Trilinear, Anistropic
    }

    enum class TextureWrapMode {
        ClampToEdge, ClampToBorder, Repeat, MirrorRepeat
    }

    fun bind() {}
    fun unbind() {}
}