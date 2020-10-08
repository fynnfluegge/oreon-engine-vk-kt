package org.oreon.core.instanced

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class InstancedHandler {
    var lock: Lock = ReentrantLock()
    var condition = lock.newCondition()
    fun signalAll() {
        lock.lock()
        try {
            condition.signalAll()
        } finally {
            lock.unlock()
        }
    }

    companion object {
        var instance: InstancedHandler? = null
            get() {
                if (field == null) {
                    field = InstancedHandler()
                }
                return field
            }
            private set
    }
}