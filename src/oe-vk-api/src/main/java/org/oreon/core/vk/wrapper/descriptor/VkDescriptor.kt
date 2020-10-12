package org.oreon.core.vk.wrapper.descriptor

import org.oreon.core.vk.descriptor.DescriptorSet
import org.oreon.core.vk.descriptor.DescriptorSetLayout

open class VkDescriptor() {

    lateinit var descriptorSet: DescriptorSet
    lateinit var descriptorSetLayout: DescriptorSetLayout

    constructor(descriptorSet: DescriptorSet,
                descriptorSetLayout: DescriptorSetLayout) : this(){
        this.descriptorSet = descriptorSet
        this.descriptorSetLayout = descriptorSetLayout
    }

    fun destroy() {
        descriptorSet.destroy()
        descriptorSetLayout.destroy()
    }
}