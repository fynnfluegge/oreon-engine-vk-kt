package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import org.oreon.core.image.ImageMetaData
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.command.SubmitInfo

class ImageCopyCmdBuffer(device: VkDevice?, commandPool: Long) : CommandBuffer(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
    fun record(stagingBuffer: Long, image: Long, metaData: ImageMetaData) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        copyBufferToImageCmd(stagingBuffer, image,
                metaData.width, metaData.height, 1)
        finishRecord()
    }

    fun submit(queue: VkQueue?) {
        val submitInfo = SubmitInfo(handlePointer)
        submitInfo.submit(queue)
    }
}