package net.ccbluex.liquidbounce.utils.customizable.editor

import java.awt.Color
import java.awt.Font

/**
 * Win95 风格主题常量
 */
object EditorTheme {

    // ===== 颜色 =====
    val BACKGROUND = Color(192, 192, 192)          // 标准 Win95 灰色
    val DARK_BACKGROUND = Color(128, 128, 128)     // 深灰色
    val WHITE = Color(255, 255, 255)
    val BLACK = Color(0, 0, 0)
    val DARKEST = Color(64, 64, 64)                // 最暗灰色（阴影）
    val HIGHLIGHT = Color(255, 255, 255)           // 高亮白
    val SHADOW = Color(128, 128, 128)              // 阴影灰

    // ===== 标题栏 =====
    val TITLE_BAR_ACTIVE = Color(0, 0, 128)        // 经典深蓝标题栏
    val TITLE_BAR_INACTIVE = Color(128, 128, 128)  // 非激活灰色标题栏
    val TITLE_TEXT = Color(255, 255, 255)
    val TITLE_TEXT_INACTIVE = Color(192, 192, 192)

    // ===== 节点颜色 =====
    val NODE_EVENT = Color(200, 200, 255)          // 事件节点 - 淡蓝
    val NODE_TARGET = Color(200, 255, 200)         // 目标节点 - 淡绿
    val NODE_ROTATION = Color(255, 220, 180)       // 旋转节点 - 淡橙
    val NODE_BEHAVIOR = Color(255, 200, 255)       // 行为节点 - 淡紫
    val NODE_MATH = Color(200, 240, 255)           // 数学节点 - 淡青
    val NODE_CONDITION = Color(255, 220, 220)      // 条件节点 - 淡红
    val NODE_ATTACK = Color(255, 180, 180)         // 攻击节点 - 红
    val NODE_BLOCK = Color(200, 200, 200)          // 格挡节点 - 灰
    val NODE_PLAYER = Color(220, 255, 220)         // 玩家节点 - 绿
    val NODE_VARIABLE = Color(255, 255, 200)       // 变量节点 - 淡黄
    val NODE_RANDOM = Color(220, 220, 255)         // 随机节点
    val NODE_DEBUG = Color(200, 200, 200)          // 调试节点
    val NODE_DEFAULT = Color(230, 230, 230)        // 默认

    val NODE_BORDER = Color(0, 0, 0)
    val NODE_SELECTED_BORDER = Color(0, 0, 255)
    val NODE_PORT = Color(0, 0, 0)
    val NODE_EXEC_PORT = Color(80, 80, 80)

    // ===== Canvas =====
    val CANVAS_BACKGROUND = Color(255, 255, 255)
    val CANVAS_GRID = Color(220, 220, 220)
    val CONNECTION_LINE = Color(0, 0, 0)
    val CONNECTION_HIGHLIGHT = Color(0, 0, 255)

    // ===== 字体 =====
    val UI_FONT = Font("MS Sans Serif", Font.PLAIN, 11)
    val UI_FONT_BOLD = Font("MS Sans Serif", Font.BOLD, 11)
    val TITLE_FONT = Font("MS Sans Serif", Font.BOLD, 12)
    val MONO_FONT = Font("Courier New", Font.PLAIN, 11)

    // ===== 尺寸 =====
    const val NODE_WIDTH = 180
    const val NODE_MIN_HEIGHT = 60
    const val NODE_PORT_RADIUS = 5
    const val NODE_HEADER_HEIGHT = 22
    const val GRID_SIZE = 20
    const val SCROLL_UNIT = 20

    // ===== 按钮外观 =====
    const val BUTTON_RAISED = 0
    const val BUTTON_SUNKEN = 1
    const val BUTTON_FLAT = 2

    /**
     * 获取节点分类颜色
     */
    fun getNodeCategoryColor(categoryName: String): Color {
        return when (categoryName.uppercase()) {
            "EVENT" -> NODE_EVENT
            "TARGET" -> NODE_TARGET
            "ROTATION" -> NODE_ROTATION
            "BEHAVIOR" -> NODE_BEHAVIOR
            "MATH" -> NODE_MATH
            "CONDITION" -> NODE_CONDITION
            "ATTACK" -> NODE_ATTACK
            "BLOCK" -> NODE_BLOCK
            "PLAYER" -> NODE_PLAYER
            "VARIABLE" -> NODE_VARIABLE
            "RANDOM" -> NODE_RANDOM
            "DEBUG" -> NODE_DEBUG
            else -> NODE_DEFAULT
        }
    }
}