package org.oreon.core.vk.wrapper.pipeline

import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput
import java.nio.LongBuffer

class GraphicsPipeline : VkPipeline {
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
        setColorBlendState()
        setDepthAndStencilTest(true)
        setDynamicState()
        setLayout(layout)
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }

    constructor(device: VkDevice?, shaderPipeline: ShaderPipeline?,
                vertexInput: VkVertexInput?, topology: Int, layout: LongBuffer?, width: Int, height: Int,
                renderPass: Long, colorAttachmentCount: Int, samples: Int) : super(device!!) {
        setVertexInput(vertexInput!!)
        setInputAssembly(topology)
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
        createGraphicsPipeline(shaderPipeline!!, renderPass)
    }
}