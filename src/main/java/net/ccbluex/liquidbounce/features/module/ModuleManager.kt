/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.command.CommandManager.registerCommand
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.`fun`.AutoL
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.FullDisabler
import net.ccbluex.liquidbounce.features.module.modules.`fun`.SkinDerp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.SkyBlockPerformanceMode
import net.ccbluex.liquidbounce.features.module.modules.`fun`.SnakeGame
import net.ccbluex.liquidbounce.features.module.modules.`fun`.DrugHallucination
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Myopia
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.music.MusicPlayer
import net.ccbluex.liquidbounce.features.module.modules.client.*
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.features.module.modules.world.Timer
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold2
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import java.util.*

private val MODULE_REGISTRY = TreeSet { m1: Module, m2: Module ->
    when {
        m1 == SkyBlockPerformanceMode -> -1
        m2 == SkyBlockPerformanceMode -> 1
        else -> m1.name.compareTo(m2.name)
    }
}

object ModuleManager : Listenable, Collection<Module> by MODULE_REGISTRY {

    fun getModules(): List<Module> = MODULE_REGISTRY.toList()

    /**
     * Register all modules
     */
    fun registerModules() {
        LOGGER.info("[ModuleManager] Loading modules...")

        // Register modules
        val modules = arrayOf(
            AbortBreaking,
            Aimbot,
            Ambience,
            Animations,
            AntiAFK,
            AntiBlind,
            AntiBot,
            AntiBounce,
            AntiCactus,
            AnticheatDetector,
            AntiExploit,
            AntiHunger,
            AntiFireball,
            AntiVoid,
            BlockNotify,
            AtAllProvider,
            AttackEffect2,
            AttackEffect,
            AutoAccount,
            AutoArmor,
            ArmorBreaker,
            AutoBow,
            AutoBreak,
            AutoClicker,
            AutoDisable,
            AutoFish,
            AutoProjectile,
            AutoPlay,
            AutoLeave,
            AutoPot,
            AutoRespawn,
            AutoRod,
            AutoSoup,
            AutoTool,
            AutoWalk,
            AutoWeapon,
            AvoidHazards,
            Backtrack,
            BAHalo,
            BedDefender,
            BedGodMode,
            BedPlates,
            BedProtectionESP,
            Blink,
            BlockESP,
            BlockOverlay,
            PointerESP,
            ProjectileAimbot,
            Breadcrumbs,
            BufferSpeed,
            CameraClip,
            CameraView,
            Cape,
            Chams,
            ChestAura,
            ChatPrefix,
            ChestStealer,
            CivBreak,
            ClickGUI,
            Clip,
            ColorMixer,
            ComponentOnHover,
            ConsoleSpammer,
            Criticals,
            Damage,
            DashTrail,
            Derp,
            ESP2D,
            ESP,
            DamageESP,
            Eagle,
            FakeLag,
            FastBow,
            FastBreak,
            FastClimb,
            FastPlace,
            FastStairs,
            FastUse,
            FlagCheck,
            Fly,
            ForceUnicodeChat,
            FreeCam,
            Freeze,
            Fucker,
            Fullbright,
            GameDetector,
            Ghost,
            Gapple,
            GhostHand,
            GodMode,
            HUD,
            HighJump,
            HitBox,
            IceSpeed,
            Ignite,
            InventoryCleaner,
            Insult,
            InventoryMove,
            Island,
            MVPDisplay,
            ItemESP,
            ItemPhysics,
            ItemTeleport,
            KeepAlive,
            KeepContainer,
            KeepTabList,
            KeyPearl,
            Kick,
            KillAura,
            RageBot,
            LagRange,
            LiquidWalk,
            Liquids,
            LongJump,
            MidClick,
            MoreCarry,
            MultiActions,
            NameProtect,
            NameTags,
            NoBob,
            NoClip,
            NoFOV,
            NoFall,
            NoFluid,
            NoFriends,
            NoHurtCam,
            NoJumpDelay,
            NoPitchLimit,
            NoRotateSet,
            NoSlotSet,
            NoSlow,
            NoSlowBreak,
            NoSwing,
            Notifier,
            ServerInfo,
            NoWeb,
            Nuker,
            PacketDebugger,
            Parkour,
            PerfectHorseJump,
            Phase,
            PingSpoof,
            PortalMenu,
            PotionSaver,
            PotionSpoof,
            Projectiles,
            ProphuntESP,
            Reach,
            Refill,
            Regen,
            NewGUI,
            PacketLogHUD,
            PotionEffect,
            TargetMark,
            ResourcePackSpoof,
            ReverseStep,
            Rotations,
            SafeWalk,
            Scaffold,
            Scaffold2,
            ServerCrasher,
            SkinDerp,
            SlimeJump,
            Sneak,
            Spammer,
            Sound,
            HUDEdit,
            ThemeManager,
            Speed,
            Sprint,
            StaffDetector,
            Step,
            StorageESP,
            Strafe,
            SuperKnockback,
            Teleport,
            TeleportHit,
            TNTBlock,
            TNTESP,
            TNTTimer,
            Teams,
            TargetManager,
            TimerRange,
            Timer,
            RawInput,
            Tracers,
            TrueSight,
            VehicleOneHit,
            Velocity,
            WallClimb,
            XRay,
            Zoot,
            KeepSprint,
            ClientFixes,
            MoreKB,
            WTap,
            TPAura,
            Disabler,
            OverrideRaycast,
            TickBase,
            RotationRecorder,
            ForwardTrack,
            FreeLook,
            SilentHotbarModule,
            SnakeGame,
            AutoL,
            FullDisabler,
            MusicPlayer,
            MoveFix,
            SkyBlockPerformanceMode,
            DamageParticle,
            HitBubbles,
            JumpCircle,
            FireFlies,
            Glint,
            FollowTargetHud,
            MotionBlur,
            DrugHallucination,
            Myopia,
            SmartBlink,
            WorldReplace,
            Star,
            DeathAnimation,
            BlockBreakFX,
            BlockPlaceFX,
            FireballTrajectory,
            AutoBlock,
            AntiKnockBack
        )

        registerModules(modules = modules)

        LOGGER.info("[ModuleManager] Loaded ${modules.size} modules.")
    }

    /**
     * Register [module]
     */
    fun registerModule(module: Module) {
        MODULE_REGISTRY += module
        generateCommand(module)
    }

    /**
     * Register a list of modules
     */
    @SafeVarargs
    fun registerModules(vararg modules: Module) = modules.forEach(this::registerModule)

    /**
     * Unregister module
     */
    fun unregisterModule(module: Module) {
        MODULE_REGISTRY.remove(module)
        module.onUnregister()
    }

    /**
     * Generate command for [module]
     */
    internal fun generateCommand(module: Module) {
        val values = module.values

        if (values.isEmpty())
            return

        registerCommand(ModuleCommand(module, values))
    }

    /**
     * Get module by [moduleClass]
     */
    operator fun get(moduleClass: Class<out Module>) = MODULE_REGISTRY.find { it.javaClass === moduleClass }

    /**
     * Get module by [moduleName]
     */
    operator fun get(moduleName: String) = MODULE_REGISTRY.find { it.name.equals(moduleName, ignoreCase = true) }

    /**
     * Get modules by [category]
     */
    operator fun get(category: Category) = MODULE_REGISTRY.filter { it.category === category }

    @Deprecated(message = "Only for outdated scripts", replaceWith = ReplaceWith("get(moduleClass)"))
    fun getModule(moduleClass: Class<out Module>) = get(moduleClass)

    @Deprecated(message = "Only for outdated scripts", replaceWith = ReplaceWith("get(moduleName)"))
    fun getModule(moduleName: String) = get(moduleName)

    /**
     * Handle incoming key presses
     */
    private val onKey = handler<KeyEvent> { event ->
        MODULE_REGISTRY.forEach { if (it.keyBind == event.key) it.toggle() }
    }

}
