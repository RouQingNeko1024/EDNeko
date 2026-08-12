package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils

object TargetManager : Module("TargetManager", Category.CLIENT, gameDetecting = false, defaultHidden = false) {

    private val player by boolean("玩家", true).onChanged { EntityUtils.Targets.player = it }
    private val mob by boolean("生物", true).onChanged { EntityUtils.Targets.mob = it }
    private val animal by boolean("动物", false).onChanged { EntityUtils.Targets.animal = it }
    private val invisible by boolean("隐身的玩家", false).onChanged { EntityUtils.Targets.invisible = it }
    private val dead by boolean("(指小游戏)死亡的玩家", false).onChanged { EntityUtils.Targets.dead = it }

    override fun onEnable() {
        EntityUtils.Targets.player = player
        EntityUtils.Targets.mob = mob
        EntityUtils.Targets.animal = animal
        EntityUtils.Targets.invisible = invisible
        EntityUtils.Targets.dead = dead
    }
}
