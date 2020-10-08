package org.oreon.core.vk.context

import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor
import java.util.*

class VkResources {
    var offScreenFbo: VkFrameBufferObject? = null
    var reflectionFbo: VkFrameBufferObject? = null
    var refractionFbo: VkFrameBufferObject? = null
    var transparencyFbo: VkFrameBufferObject? = null
    val descriptors: Map<VkDescriptorName, VkDescriptor> = HashMap()

    enum class VkDescriptorName {
        CAMERA, DIRECTIONAL_LIGHT
    }
}