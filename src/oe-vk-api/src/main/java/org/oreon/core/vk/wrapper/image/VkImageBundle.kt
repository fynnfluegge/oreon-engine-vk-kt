package org.oreon.core.vk.wrapper.image

import org.oreon.core.vk.image.VkImage
import org.oreon.core.vk.image.VkImageView
import org.oreon.core.vk.image.VkSampler

open class VkImageBundle {
    lateinit var image: VkImage
        protected set
    lateinit var imageView: VkImageView
        protected set
    lateinit var sampler: VkSampler
        private set

    constructor() {}
    constructor(image: VkImage, imageView: VkImageView) {
        this.image = image
        this.imageView = imageView
    }

    constructor(image: VkImage, imageView: VkImageView, sampler: VkSampler) {
        this.image = image
        this.imageView = imageView
        this.sampler = sampler
    }

    fun destroy() {
        if (sampler != null) {
            sampler!!.destroy()
        }
        imageView!!.destroy()
        image!!.destroy()
    }
}