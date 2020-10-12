package org.oreon.core.vk.wrapper.descriptor

import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout

@Getter
@AllArgsConstructor
@NoArgsConstructor
open class VkDescriptor {
    protected var descriptorSet: DescriptorSet? = null
    protected var descriptorSetLayout: DescriptorSetLayout? = null
    fun destroy() {
        descriptorSet!!.destroy()
        descriptorSetLayout!!.destroy()
    }
}