package org.oreon.core.vk.wrapper.pipeline

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import java.nio.LongBuffer

class GraphicsTessellationPipeline : VkPipeline {
    constructor(device: VkDevice?, shaderPipeline: ShaderPipeline?,
                vertexInput: VkVertexInput?, layout: LongBuffer?, width: Int, height: Int,
                renderPass: Long, colorAttachmentCount: Int, samples: Int,
                pushConstantRange: Int, pushConstantStageFlags: Int,
                patchControlPoints: Int) : super(device!!) {
        setVertexInput(vertexInput!!)
        setPushConstantsRange(pushConstantStageFlags, pushConstantRange)
        setInputAssembly(VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST)
        setViewportAndScissor(width, height)
        setRasterizer()
        setMultisamplingState(samples)
        for (i in 0 until colorAttachmentCount) {
            addColorBlendAttachment()
        }
        setColorBlendState()
        setDepthAndStencilTest(true)
        setDynamicState()
        setLayout(layout)
        setTessellationState(patchControlPoints)
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }

    constructor(device: VkDevice?, shaderPipeline: ShaderPipeline?,
                vertexInput: VkVertexInput?, layout: LongBuffer?, width: Int, height: Int,
                renderPass: Long, colorAttachmentCount: Int,
                samples: Int, patchControlPoints: Int) : super(device!!) {
        setVertexInput(vertexInput!!)
        setInputAssembly(VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST)
        setViewportAndScissor(width, height)
        setRasterizer()
        setMultisamplingState(samples)
        for (i in 0 until colorAttachmentCount) {
            addColorBlendAttachment()
        }
        setColorBlendState()
        setDepthAndStencilTest(true)
        setDynamicState()
        setLayout(layout)
        setTessellationState(patchControlPoints)
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }
}