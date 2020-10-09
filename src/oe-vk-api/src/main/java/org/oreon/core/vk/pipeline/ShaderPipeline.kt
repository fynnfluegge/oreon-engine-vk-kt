package org.oreon.core.vk.pipeline

import lombok.Getter
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import java.util.*

class ShaderPipeline(private val device: VkDevice) {
    @Getter
    private var stages: VkPipelineShaderStageCreateInfo.Buffer? = null
    private val shaderStages: MutableList<ShaderModule> = ArrayList()
    fun createShaderPipeline() {
        stages = VkPipelineShaderStageCreateInfo.calloc(shaderStages.size)
        for (shaderStage in shaderStages) {
            stages.put(shaderStage.shaderStageInfo)
        }
        stages.flip()
    }

    fun createVertexShader(filePath: String?) {
        shaderStages.add(ShaderModule(device, filePath!!, VK10.VK_SHADER_STAGE_VERTEX_BIT))
    }

    fun createTessellationControlShader(filePath: String?) {
        shaderStages.add(ShaderModule(device, filePath!!, VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT))
    }

    fun createTessellationEvaluationShader(filePath: String?) {
        shaderStages.add(ShaderModule(device, filePath!!, VK10.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT))
    }

    fun createGeometryShader(filePath: String?) {
        shaderStages.add(ShaderModule(device, filePath!!, VK10.VK_SHADER_STAGE_GEOMETRY_BIT))
    }

    fun createFragmentShader(filePath: String?) {
        shaderStages.add(ShaderModule(device, filePath!!, VK10.VK_SHADER_STAGE_FRAGMENT_BIT))
    }

    fun addShaderModule(shaderModule: ShaderModule) {
        shaderStages.add(shaderModule)
    }

    fun destroy() {
        stages!!.free()
        for (shaderModule in shaderStages) {
            if (shaderModule.handle != -1L) {
                shaderModule.destroy()
            }
        }
    }
}