package org.oreon.core.target

abstract class FrameBufferObject {
    var height = 0
        protected set
    var width = 0
        protected set
    var colorAttachmentCount = 0
        protected set
    var depthAttachmentCount = 0
        protected set

    enum class Attachment {
        COLOR, ALPHA, NORMAL, POSITION, SPECULAR_EMISSION_DIFFUSE_SSAO_BLOOM, LIGHT_SCATTERING, DEPTH
    }
}