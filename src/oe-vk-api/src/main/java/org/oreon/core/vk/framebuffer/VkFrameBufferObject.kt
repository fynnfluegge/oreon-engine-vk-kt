package org.oreon.core.vk.framebuffer

import org.oreon.core.target.FrameBufferObject
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.pipeline.RenderPass
import org.oreon.core.vk.wrapper.image.VkImageBundle
import java.util.*

open class VkFrameBufferObject : FrameBufferObject() {

    var frameBuffer: VkFrameBuffer? = null
    var renderPass: RenderPass? = null
    var attachments = HashMap<Attachment, VkImageBundle>()

    fun getAttachmentImageView(type: Attachment): VkImageView {
        return attachments[type]!!.imageView
    }

    fun destroy() {
        frameBuffer!!.destroy()
        renderPass!!.destroy()
        for ((_, value) in attachments) {
            value.destroy()
        }
    }
}