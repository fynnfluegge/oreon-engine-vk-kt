package org.oreon.core.vk.wrapper.command

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.command.CommandBuffer
import java.nio.ByteBuffer

class ComputeCmdBuffer : CommandBuffer {
    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, descriptorSets: LongArray?,
                groupCountX: Int, groupCountY: Int, groupCountZ: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        record(pipeline, pipelineLayout, descriptorSets,
                groupCountX, groupCountY, groupCountZ, null, 0)
    }

    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, descriptorSets: LongArray?,
                groupCountX: Int, groupCountY: Int, groupCountZ: Int,
                pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        record(pipeline, pipelineLayout, descriptorSets,
                groupCountX, groupCountY, groupCountZ,
                pushConstantsData, pushConstantsStageFlags)
    }

    constructor(device: VkDevice?, commandPool: Long,
                pipeline: Long, pipelineLayout: Long, descriptorSets: LongArray?,
                groupCountX: Int, groupCountY: Int, groupCountZ: Int,
                image: Long, imageLayout: Int) : super(device!!, commandPool, VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY) {
        record(pipeline, pipelineLayout, descriptorSets,
                groupCountX, groupCountY, groupCountZ,
                image, imageLayout)
    }

    fun record(pipeline: Long, pipelineLayout: Long,
               descriptorSets: LongArray?, groupCountX: Int, groupCountY: Int, groupCountZ: Int,
               pushConstantsData: ByteBuffer?, pushConstantsStageFlags: Int) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        pushConstantsData?.let { pushConstantsCmd(pipelineLayout, pushConstantsStageFlags, it) }
        bindComputePipelineCmd(pipeline)
        bindComputeDescriptorSetsCmd(pipelineLayout, descriptorSets!!)
        dispatchCmd(groupCountX, groupCountY, groupCountZ)
        finishRecord()
    }

    fun record(pipeline: Long, pipelineLayout: Long,
               descriptorSets: LongArray?, groupCountX: Int, groupCountY: Int, groupCountZ: Int,
               image: Long, imageLayout: Int) {
        beginRecord(VK10.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
        clearColorImageCmd(image, imageLayout)
        bindComputePipelineCmd(pipeline)
        bindComputeDescriptorSetsCmd(pipelineLayout, descriptorSets!!)
        dispatchCmd(groupCountX, groupCountY, groupCountZ)
        finishRecord()
    }
}