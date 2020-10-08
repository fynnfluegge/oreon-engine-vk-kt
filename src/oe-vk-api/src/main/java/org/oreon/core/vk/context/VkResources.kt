package org.oreon.core.vk.context

import lombok.Getter
import lombok.Setter
import org.oreon.core.vk.framebuffer.VkFrameBufferObject
import org.oreon.core.vk.wrapper.descriptor.VkDescriptor
import java.util.*

@Getter
@Setter
class VkResources {
    private val offScreenFbo: VkFrameBufferObject? = null
    private val reflectionFbo: VkFrameBufferObject? = null
    private val refractionFbo: VkFrameBufferObject? = null
    private val transparencyFbo: VkFrameBufferObject? = null
    private val descriptors: Map<VkDescriptorName, VkDescriptor> = HashMap()

    enum class VkDescriptorName {
        CAMERA, DIRECTIONAL_LIGHT
    }
}