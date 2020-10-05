package org.oreon.core.vk.device;

public class VkDeviceBundle {

	public PhysicalDevice getPhysicalDevice() {
		return physicalDevice;
	}

	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public void setPhysicalDevice(PhysicalDevice physicalDevice) {
		this.physicalDevice = physicalDevice;
	}

	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	private PhysicalDevice physicalDevice;
	private LogicalDevice logicalDevice;

	public VkDeviceBundle(PhysicalDevice physicalDevice, LogicalDevice logicalDevice){
		this.physicalDevice = physicalDevice;
		this.logicalDevice = logicalDevice;
	}

}
