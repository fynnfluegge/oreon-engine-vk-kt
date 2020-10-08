package org.oreon.core.math

import org.oreon.core.math.Complex

class Complex(var real: Float, var im: Float) {
    fun add(c: Complex) {
        real += c.real
        im += c.im
    }

    fun sub(c: Complex) {
        real -= c.real
        im -= c.im
    }

    fun mul(c: Complex) {
        real = real * c.real - im * c.im
        im = real * c.im + im * c.real
    }

    fun mul(i: Float) {
        real *= i
        im *= i
    }
}