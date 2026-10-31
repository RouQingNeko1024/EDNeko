package net.ccbluex.liquidbounce.utils.math

import kotlin.math.roundToInt

object MathFunction {
    fun pow(base: Any, exponent: Any): Number {
        // 安全转换为 Double
        val baseDouble = when (base) {
            is Number -> base.toDouble()
            else -> base.toString().toDoubleOrNull() ?: 0.0
        }

        val expDouble = when (exponent) {
            is Number -> exponent.toDouble()
            else -> exponent.toString().toDoubleOrNull() ?: 0.0
        }

        // 如果指数是整数，使用优化算法
        return if (exponent is Int) {
            val exp = exponent
            if (exp >= 0) {
                // 使用循环替代位运算
                var result = 1.0
                var b = baseDouble
                var e = exp

                while (e > 0) {
                    // 判断是否为奇数
                    if (e % 2 == 1) {
                        result *= b
                    }
                    b *= b
                    e /= 2  // 替代 shr
                }
                result
            } else {
                1.0 / pow(baseDouble, -exp).toDouble()
            }
        } else {
            // 其他情况使用数学库
            baseDouble.pow(expDouble)
        }
    }
    /**
     * 数值类型的扩展函数版本
     */
    fun Number.pow(exponent: Number): Number = pow(this, exponent)

    /**
     * 具体类型的扩展函数（更精确的类型推断）
     */
    fun Int.pow(exponent: Int): Int {
        if (exponent >= 0) {
            var result = 1
            var b = this
            var e = exponent

            while (e > 0) {
                if (e % 2 == 1) {
                    result *= b
                }
                b *= b
                e /= 2
            }
            return result
        } else {
            return (1.0 / this.pow(-exponent).toDouble()).roundToInt()
        }
    }

    fun Double.pow(exponent: Int): Double {
        if (exponent >= 0) {
            var result = 1.0
            var b = this
            var e = exponent

            while (e > 0) {
                if (e % 2 == 1) {
                    result *= b
                }
                b *= b
                e /= 2
            }
            return result
        } else {
            return 1.0 / this.pow(-exponent)
        }
    }

    fun Float.pow(exponent: Int): Float {
        return this.toDouble().pow(exponent).toFloat()
    }

    fun Long.pow(exponent: Int): Long {
        if (exponent >= 0) {
            var result = 1L
            var b = this
            var e = exponent

            while (e > 0) {
                if (e % 2 == 1) {
                    result *= b
                }
                b *= b
                e /= 2
            }
            return result
        } else {
            return (1.0 / this.toDouble().pow(-exponent)).toLong()
        }
    }
}