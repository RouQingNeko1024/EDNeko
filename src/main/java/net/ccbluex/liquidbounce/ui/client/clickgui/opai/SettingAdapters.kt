package net.ccbluex.liquidbounce.ui.client.clickgui.opai

import net.ccbluex.liquidbounce.config.*
import java.awt.Color

object SettingAdapters {

    abstract class Setting<T>(val value: Value<T>) {
        open fun getDisplayName(): String = value.name
        open fun getValue(): T = value.get()
        open fun setValue(val1: T) { value.set(val1, true) }
        open fun isAvailable(): Boolean = value.shouldRender()
        open fun getGroup(): SettingGroup? = null
    }

    class BoolSetting(value: BoolValue) : Setting<Boolean>(value)

    class IntSetting(val intValue: IntValue) : Setting<Int>(intValue) {
        val min = intValue.minimum
        val max = intValue.maximum
        val step = 1
        fun isPercentageMode(): Boolean = intValue.suffix == "%"
        
        override fun getValue(): Int = intValue.get()
        override fun setValue(val1: Int) { intValue.set(val1, true) }
    }

    class DoubleSetting(val floatValue: FloatValue) : Setting<Float>(floatValue) {
        val min = floatValue.minimum.toDouble()
        val max = floatValue.maximum.toDouble()
        val step = 0.01
        
        override fun getDisplayName(): String = floatValue.name
        fun getDoubleValue(): Double = floatValue.get().toDouble()
        fun setDoubleValue(val1: Double) { floatValue.set(val1.toFloat(), true) }
        override fun isAvailable(): Boolean = floatValue.shouldRender()
        fun isPercentageMode(): Boolean = floatValue.suffix == "%"
    }

    class IntRangeSetting(val intRangeValue: IntRangeValue) : Setting<IntRange>(intRangeValue) {
        val min = intRangeValue.minimum
        val max = intRangeValue.maximum
        override fun getValue(): IntRange = intRangeValue.get()
        override fun setValue(val1: IntRange) { intRangeValue.set(val1, true) }
        override fun isAvailable(): Boolean = intRangeValue.shouldRender()
        fun getMinimum(): Int = intRangeValue.get().first
        fun getMaximum(): Int = intRangeValue.get().last
        fun setFirst(value: Int) { intRangeValue.setFirst(value, true) }
        fun setLast(value: Int) { intRangeValue.setLast(value, true) }
        fun getSelectedRangeSlider(): RangeSlider? = intRangeValue.lastChosenSlider
    }

    class DoubleRangeSetting(val floatRangeValue: FloatRangeValue) : Setting<ClosedFloatingPointRange<Float>>(floatRangeValue) {
        val min = floatRangeValue.minimum
        val max = floatRangeValue.maximum
        override fun getValue(): ClosedFloatingPointRange<Float> = floatRangeValue.get()
        override fun setValue(val1: ClosedFloatingPointRange<Float>) { floatRangeValue.set(val1, true) }
        override fun isAvailable(): Boolean = floatRangeValue.shouldRender()
        fun getMinimum(): Float = floatRangeValue.get().start
        fun getMaximum(): Float = floatRangeValue.get().endInclusive
        fun setFirst(value: Float) { floatRangeValue.setFirst(value, true) }
        fun setLast(value: Float) { floatRangeValue.setLast(value, true) }
        fun getSelectedRangeSlider(): RangeSlider? = floatRangeValue.lastChosenSlider
    }

    class EnumSetting(val listValue: ListValue) : Setting<String>(listValue) {
        val modes: Array<String> = listValue.values
        fun getTranslatedValueByIndex(index: Int): String = modes[index]
        fun setMode(name: String) { setValue(name) }
        override fun getValue(): String = listValue.get()
        fun getOrdinal(): Int = modes.indexOf(getValue())
    }

    class ColorSetting(val colorValue: ColorValue) : Setting<Color>(colorValue) {
        fun isAllowAlpha(): Boolean = true
    }

    class KeybindSetting(value: Value<Int>) : Setting<Int>(value)

    class StringSetting(val textValue: TextValue) : Setting<String>(textValue)

    class ButtonSetting(val name: String, val action: Runnable?) {
        fun getDisplayName(): String = name
        fun isAvailable(): Boolean = true
        fun getGroup(): SettingGroup? = null
    }
    
    class SettingGroup(val name: String) {
        private var collapsed = false
        fun getDisplayName(): String = name
        fun isCollapsed(): Boolean = collapsed
        fun toggleCollapsed() { collapsed = !collapsed }
    }
}
