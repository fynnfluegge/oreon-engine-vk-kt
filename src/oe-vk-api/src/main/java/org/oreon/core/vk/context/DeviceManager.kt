package org.oreon.core.vk.context

import lombok.Getter
import org.oreon.core.vk.device.LogicalDevice
import org.oreon.core.vk.device.PhysicalDevice
import org.oreon.core.vk.device.VkDeviceBundle
import java.util.*

@Getter
class DeviceManager {
    private val devices: HashMap<DeviceType, VkDeviceBundle>

    enum class DeviceType {
        MAJOR_GRAPHICS_DEVICE, SECONDARY_GRAPHICS_DEVICE, COMPUTING_DEVICE, SLI_DISCRETE_DEVICE0, SLI_DISCRETE_DEVICE1
    }

    fun getDeviceBundle(deviceType: DeviceType): VkDeviceBundle? {
        return devices[deviceType]
    }

    fun getPhysicalDevice(deviceType: DeviceType): PhysicalDevice {
        return devices[deviceType]!!.physicalDevice
    }

    fun getLogicalDevice(deviceType: DeviceType): LogicalDevice {
        return devices[deviceType]!!.logicalDevice
    }

    fun addDevice(deviceType: DeviceType, deviceBundle: VkDeviceBundle) {
        devices[deviceType] = deviceBundle
    }

    init {
        devices = HashMap()
    }
}