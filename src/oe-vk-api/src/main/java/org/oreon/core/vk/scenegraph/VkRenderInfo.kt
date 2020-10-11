package org.oreon.core.vk.scenegraph

import org.oreon.core.scenegraph.NodeComponent
import org.oreon.core.vk.command.CommandBuffer
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout
import org.oreon.core.vk.pipeline.ShaderPipeline
import org.oreon.core.vk.pipeline.VkPipeline
import org.oreon.core.vk.pipeline.VkVertexInput

class VkRenderInfo(var commandBuffer: CommandBuffer? = null, var pipeline: VkPipeline? = null, val vertexInput: VkVertexInput? = null,
    val descriptorSets: List<DescriptorSet>? = null, val descriptorSetLayouts: List<DescriptorSetLayout>? = null, val shaderPipeline: ShaderPipeline? = null) : NodeComponent() {

    override fun shutdown() {
        pipeline?.destroy()
        commandBuffer?.destroy()
        shaderPipeline?.destroy()
        if (descriptorSetLayouts != null) {
            for (layout in descriptorSetLayouts) {
                if (layout.handle != -1L) layout.destroy()
            }
        }
        if (descriptorSets != null) {
            for (set in descriptorSets) {
                if (set.handle != -1L) set.destroy()
            }
        }
    }
}