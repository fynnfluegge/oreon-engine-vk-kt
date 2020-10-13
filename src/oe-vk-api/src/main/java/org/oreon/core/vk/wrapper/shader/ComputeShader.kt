package org.oreon.core.vk.wrapper.shader

import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VkDevice
import org.oreon.core.vk.pipeline.ShaderModule

class ComputeShader(device: VkDevice?, filePath: String?) : ShaderModule(device!!, filePath!!, VK10.VK_SHADER_STAGE_COMPUTE_BIT)