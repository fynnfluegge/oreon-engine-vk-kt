package org.oreon.core.vk.pipeline

import lombok.Getter
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.oreon.core.vk.util.VkUtil
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.util.*

open class VkPipeline(private val device: VkDevice) {
    @Getter
    private var handle: Long = 0

    @Getter
    private var layoutHandle: Long = 0
    private var vertexInputState: VkPipelineVertexInputStateCreateInfo? = null
    private var pushConstantRange: VkPushConstantRange.Buffer? = null
    private var inputAssembly: VkPipelineInputAssemblyStateCreateInfo? = null
    private var viewportAndScissorState: VkPipelineViewportStateCreateInfo? = null
    private var rasterizer: VkPipelineRasterizationStateCreateInfo? = null
    private var multisamplingState: VkPipelineMultisampleStateCreateInfo? = null
    private var colorBlending: VkPipelineColorBlendStateCreateInfo? = null
    private var colorBlendAttachmentStates: VkPipelineColorBlendAttachmentState.Buffer? = null
    private var depthStencilState: VkPipelineDepthStencilStateCreateInfo? = null
    private var dynamicState: VkPipelineDynamicStateCreateInfo? = null
    private var tessellationState: VkPipelineTessellationStateCreateInfo? = null
    private var viewport: VkViewport.Buffer? = null
    private var scissor: VkRect2D.Buffer? = null
    private var pDynamicStates: IntBuffer? = null
    var colorBlendAttachments: MutableList<VkPipelineColorBlendAttachmentState> = ArrayList()
    fun createGraphicsPipeline(shaderPipeline: ShaderPipeline, renderPass: Long) {
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(shaderPipeline.stages)
                .pVertexInputState(vertexInputState)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportAndScissorState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisamplingState)
                .pDepthStencilState(depthStencilState)
                .pColorBlendState(colorBlending)
                .pDynamicState(null)
                .pTessellationState(tessellationState)
                .layout(layoutHandle)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK10.VK_NULL_HANDLE)
                .basePipelineIndex(-1)
        val pPipelines = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreateGraphicsPipelines(device, VK10.VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines)
        handle = pPipelines[0]
        vertexInputState!!.free()
        inputAssembly!!.free()
        viewportAndScissorState!!.free()
        rasterizer!!.free()
        multisamplingState!!.free()
        colorBlending!!.free()
        colorBlendAttachmentStates!!.free()
        depthStencilState!!.free()
        dynamicState!!.free()
        viewport!!.free()
        scissor!!.free()
        MemoryUtil.memFree(pDynamicStates)
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create pipeline: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun createComputePipeline(shader: ShaderModule) {
        val pipelineCreateInfo = VkComputePipelineCreateInfo.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                .stage(shader.shaderStageInfo)
                .layout(layoutHandle)
        val pPipelines = MemoryUtil.memAllocLong(1)
        VkUtil.vkCheckResult(VK10.vkCreateComputePipelines(device,
                VK10.VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines))
        handle = pPipelines[0]
        pipelineCreateInfo.free()
    }

    fun setLayout(pLayouts: LongBuffer?) {
        val pipelineLayout = VkPipelineLayoutCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pNext(0)
                .pSetLayouts(pLayouts)
                .pPushConstantRanges(pushConstantRange)
        val pPipelineLayout = MemoryUtil.memAllocLong(1)
        val err = VK10.vkCreatePipelineLayout(device, pipelineLayout, null, pPipelineLayout)
        layoutHandle = pPipelineLayout[0]
        MemoryUtil.memFree(pPipelineLayout)
        pipelineLayout.free()
        if (err != VK10.VK_SUCCESS) {
            throw AssertionError("Failed to create pipeline layout: " + VkUtil.translateVulkanResult(err))
        }
    }

    fun setVertexInput(vertexInput: VkVertexInput) {
        vertexInputState = VkPipelineVertexInputStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pNext(0)
                .pVertexBindingDescriptions(vertexInput.bindingDescription)
                .pVertexAttributeDescriptions(vertexInput.attributeDescriptions)
    }

    fun setPushConstantsRange(stageFlags: Int, size: Int) {
        pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(stageFlags)
                .size(size)
                .offset(0)
    }

    fun setInputAssembly(topology: Int) {
        inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(topology)
                .primitiveRestartEnable(false)
    }

    fun setViewportAndScissor(width: Int, height: Int) {
        viewport = VkViewport.calloc(1)
                .x(0f)
                .y(0f)
                .width(width.toFloat())
                .height(height.toFloat())
                .minDepth(0.0f)
                .maxDepth(1.0f)
        scissor = VkRect2D.calloc(1)
        scissor.extent()[width] = height
        scissor.offset()[0] = 0
        viewportAndScissorState = VkPipelineViewportStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .pViewports(viewport)
                .scissorCount(1)
                .pScissors(scissor)
    }

    fun setRasterizer() {
        rasterizer = VkPipelineRasterizationStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                .cullMode(VK10.VK_CULL_MODE_NONE)
                .frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .rasterizerDiscardEnable(false)
                .lineWidth(1.0f)
                .depthClampEnable(false)
                .depthBiasEnable(false)
                .depthBiasConstantFactor(0f)
                .depthBiasSlopeFactor(0f)
                .depthBiasClamp(0f)
    }

    fun setMultisamplingState(samples: Int) {
        multisamplingState = VkPipelineMultisampleStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VkUtil.getSampleCountBit(samples))
                .pSampleMask(null)
                .minSampleShading(1f)
                .alphaToCoverageEnable(false)
                .alphaToOneEnable(false)
    }

    fun addColorBlendAttachment() {
        val colorWriteMask = VkPipelineColorBlendAttachmentState.calloc()
                .blendEnable(false)
                .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT or VK10.VK_COLOR_COMPONENT_G_BIT
                        or VK10.VK_COLOR_COMPONENT_B_BIT or VK10.VK_COLOR_COMPONENT_A_BIT)
        colorBlendAttachments.add(colorWriteMask)
    }

    fun setColorBlendState() {
        colorBlendAttachmentStates = VkPipelineColorBlendAttachmentState.calloc(colorBlendAttachments.size)
        for (colorBlendAttachment in colorBlendAttachments) {
            colorBlendAttachment.blendEnable(false)
            colorBlendAttachmentStates.put(colorBlendAttachment)
        }
        colorBlendAttachmentStates.flip()
        colorBlending = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .pAttachments(colorBlendAttachmentStates)
        for (colorBlendAttachment in colorBlendAttachments) {
            colorBlendAttachment.free()
        }
    }

    fun setColorBlendState(srcColorBlendFactor: Int, dstColorBlendFactor: Int,
                           srcAlphaBlendFactor: Int, dstAlphaBlendFactor: Int, colorBlendOp: Int, alphaBlendOp: Int) {
        colorBlendAttachmentStates = VkPipelineColorBlendAttachmentState.calloc(colorBlendAttachments.size)
        for (colorBlendAttachment in colorBlendAttachments) {
            colorBlendAttachment
                    .blendEnable(true)
                    .srcColorBlendFactor(srcColorBlendFactor)
                    .dstColorBlendFactor(dstColorBlendFactor)
                    .colorBlendOp(colorBlendOp)
                    .srcAlphaBlendFactor(srcAlphaBlendFactor)
                    .dstAlphaBlendFactor(dstAlphaBlendFactor)
                    .alphaBlendOp(alphaBlendOp)
                    .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT or VK10.VK_COLOR_COMPONENT_G_BIT
                            or VK10.VK_COLOR_COMPONENT_B_BIT or VK10.VK_COLOR_COMPONENT_A_BIT)
            colorBlendAttachmentStates.put(colorBlendAttachment)
        }
        colorBlendAttachmentStates.flip()
        colorBlending = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .pAttachments(colorBlendAttachmentStates)
        for (colorBlendAttachment in colorBlendAttachments) {
            colorBlendAttachment.free()
        }
    }

    fun setDepthAndStencilTest(depthTestEnable: Boolean) {
        depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(depthTestEnable)
                .depthWriteEnable(true)
                .depthCompareOp(VK10.VK_COMPARE_OP_LESS)
                .depthBoundsTestEnable(false)
                .stencilTestEnable(false)
        depthStencilState.back()
                .failOp(VK10.VK_STENCIL_OP_KEEP)
                .passOp(VK10.VK_STENCIL_OP_KEEP)
                .compareOp(VK10.VK_COMPARE_OP_ALWAYS)
        depthStencilState.front(depthStencilState.back())
    }

    fun setDynamicState() {
        pDynamicStates = MemoryUtil.memAllocInt(2)
        pDynamicStates.put(VK10.VK_DYNAMIC_STATE_VIEWPORT)
        pDynamicStates.put(VK10.VK_DYNAMIC_STATE_SCISSOR)
        pDynamicStates.flip()
        dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(pDynamicStates)
    }

    fun setTessellationState(patchControlPoints: Int) {
        tessellationState = VkPipelineTessellationStateCreateInfo.calloc()
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO)
                .flags(0)
                .patchControlPoints(patchControlPoints)
    }

    fun destroy() {
        VK10.vkDestroyPipelineLayout(device, layoutHandle, null)
        VK10.vkDestroyPipeline(device, handle, null)
    }
}