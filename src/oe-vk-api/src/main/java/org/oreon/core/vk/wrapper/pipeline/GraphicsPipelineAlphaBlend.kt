package org.oreon.core.vk.wrapper.pipeline

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import java.nio.LongBuffer

class GraphicsPipelineAlphaBlend : VkPipeline {
    constructor(device: VkDevice?, shaderPipeline: ShaderPipeline?,
                vertexInput: VkVertexInput?, topology: Int, layout: LongBuffer?, width: Int, height: Int,
                renderPass: Long, colorAttachmentCount: Int, samples: Int,
                srcAlphaBlendFactor: Int, dstAlphaBlendFactor: Int, alphaBlendOp: Int) : super(device!!) {
        setVertexInput(vertexInput!!)
        setInputAssembly(topology)
        setViewportAndScissor(width, height)
        setRasterizer()
        setMultisamplingState(samples)
        for (i in 0 until colorAttachmentCount) {
            addColorBlendAttachment()
        }
        setColorBlendState(VK10.VK_BLEND_FACTOR_SRC_ALPHA, VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK10.VK_BLEND_OP_ADD, VK10.VK_BLEND_FACTOR_ONE, VK10.VK_BLEND_FACTOR_ZERO, VK10.VK_BLEND_OP_ADD)
        setDepthAndStencilTest(true)
        setDynamicState()
        setLayout(layout)
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }

    constructor(device: VkDevice?, shaderPipeline: ShaderPipeline?,
                vertexInput: VkVertexInput?, topology: Int, layout: LongBuffer?, width: Int, height: Int,
                renderPass: Long, colorAttachmentCount: Int, samples: Int,
                pushConstantRange: Int, pushConstantStageFlags: Int) : super(device!!) {
        setVertexInput(vertexInput!!)
        setInputAssembly(topology)
        setPushConstantsRange(pushConstantStageFlags, pushConstantRange)
        setViewportAndScissor(width, height)
        setRasterizer()
        setMultisamplingState(samples)
        for (i in 0 until colorAttachmentCount) {
            addColorBlendAttachment()
        }
        setColorBlendState(VK10.VK_BLEND_FACTOR_SRC_ALPHA, VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK10.VK_BLEND_FACTOR_ONE, VK10.VK_BLEND_FACTOR_ZERO, VK10.VK_BLEND_OP_ADD, VK10.VK_BLEND_OP_ADD)
        setDepthAndStencilTest(true)
        setDynamicState()
        setLayout(layout)
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }
}