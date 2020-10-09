package org.oreon.core.vk.pipeline

import lombok.Getter
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import org.oreon.core.model.Vertex.VertexLayout

class VkVertexInput(layout: VertexLayout?) {
    @Getter
    private var bindingDescription: VkVertexInputBindingDescription.Buffer? = null

    @Getter
    private var attributeDescriptions: VkVertexInputAttributeDescription.Buffer? = null
    private var binding = 0
    private fun createBindingDescription(binding: Int, attributeCount: Int, stride: Int) {
        this.binding = binding
        bindingDescription = VkVertexInputBindingDescription.calloc(1)
                .binding(binding)
                .stride(stride)
                .inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX)
        attributeDescriptions = VkVertexInputAttributeDescription.calloc(attributeCount)
    }

    private fun addVertexAttributeDescription(location: Int, format: Int, offset: Int) {
        val attributeDescription = VkVertexInputAttributeDescription.calloc()
                .binding(binding)
                .location(location)
                .format(format)
                .offset(offset)
        attributeDescriptions!!.put(location, attributeDescription)
    }

    init {
        when (layout) {
            VertexLayout.POS2D -> {
                createBindingDescription(0, 1, java.lang.Float.BYTES * 2)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32_SFLOAT, 0)
            }
            VertexLayout.POS -> {
                createBindingDescription(0, 1, java.lang.Float.BYTES * 3)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32B32_SFLOAT, 0)
            }
            VertexLayout.POS_UV -> {
                createBindingDescription(0, 2, java.lang.Float.BYTES * 5)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32B32_SFLOAT, 0)
                addVertexAttributeDescription(1, VK10.VK_FORMAT_R32G32_SFLOAT, java.lang.Float.BYTES * 3)
            }
            VertexLayout.POS2D_UV -> {
                createBindingDescription(0, 2, java.lang.Float.BYTES * 4)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32_SFLOAT, 0)
                addVertexAttributeDescription(1, VK10.VK_FORMAT_R32G32_SFLOAT, java.lang.Float.BYTES * 2)
            }
            VertexLayout.POS_NORMAL -> {
                createBindingDescription(0, 2, java.lang.Float.BYTES * 6)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32B32_SFLOAT, 0)
                addVertexAttributeDescription(1, VK10.VK_FORMAT_R32G32B32_SFLOAT, java.lang.Float.BYTES * 3)
            }
            VertexLayout.POS_NORMAL_UV -> {
                createBindingDescription(0, 3, java.lang.Float.BYTES * 8)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32B32_SFLOAT, 0)
                addVertexAttributeDescription(1, VK10.VK_FORMAT_R32G32B32_SFLOAT, java.lang.Float.BYTES * 3)
                addVertexAttributeDescription(2, VK10.VK_FORMAT_R32G32_SFLOAT, java.lang.Float.BYTES * 6)
            }
            VertexLayout.POS_NORMAL_UV_TAN_BITAN -> {
                createBindingDescription(0, 5, java.lang.Float.BYTES * 14)
                addVertexAttributeDescription(0, VK10.VK_FORMAT_R32G32B32_SFLOAT, 0)
                addVertexAttributeDescription(1, VK10.VK_FORMAT_R32G32B32_SFLOAT, java.lang.Float.BYTES * 3)
                addVertexAttributeDescription(2, VK10.VK_FORMAT_R32G32_SFLOAT, java.lang.Float.BYTES * 6)
                addVertexAttributeDescription(3, VK10.VK_FORMAT_R32G32B32_SFLOAT, java.lang.Float.BYTES * 8)
                addVertexAttributeDescription(4, VK10.VK_FORMAT_R32G32B32_SFLOAT, java.lang.Float.BYTES * 11)
            }
            else -> {
            }
        }
    }
}