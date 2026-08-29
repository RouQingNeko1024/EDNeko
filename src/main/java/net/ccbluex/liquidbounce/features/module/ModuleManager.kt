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
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseComboOneHit
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseCriticals
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseVelocity
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTickBase
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTeleportAura
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseThrowableAura
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseWatchdogTPAura
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseLegitReach
import net.ccbluex.liquidbounce.features.module.modules.rise.RisePiercing
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseMimic
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFences
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseKeepRange
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseKnockbackSample
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseLagBreak
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseRegen
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseRotationSnapshot
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAntiBot
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFlight
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSpeed
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseLongJump
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoSlow
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseStep
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseStrafe
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTargetStrafe
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoClip
import net.ccbluex.liquidbounce.features.module.modules.rise.RisePhase
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSneak
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSprint
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSnapTap
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseWallClimb
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseJesus
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseClipper
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAutoStuck
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseStuck
import net.ccbluex.liquidbounce.features.module.modules.rise.RisePotionExtender
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTerrainSpeed
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTeleport
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAutoMLG
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoJumpDelay
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseResourcePackSpoof
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseDisabler
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseCrasher
import net.ccbluex.liquidbounce.features.module.modules.rise.RisePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoRotate
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseGhostHand
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseGodMode
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseConsoleSpammer
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseBlockTracker
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseLightningTracker
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseStaffDetector
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAimAssist
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAimBacktrack
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseClickAssist
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseDisplacementSample
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFastPlace
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseGuiClicker
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseKeepSprint
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseLegitScaffold
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseManualKBDisplacement
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoClickDelay
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseOldWTap
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseWTap
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseESP
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNameTags
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTracers
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFullBright
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseChestESP
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFreeCam
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseFreeLook
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseBreadCrumbs
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseHurtCamera
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseKillEffect
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTargetInfo
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSessionStats
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseUnlimitedChat
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseViewBobbing
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoCameraClip
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseOreESP
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseM2DESP
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseJumpCircles
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseParticles
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAmbience
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseBedPlates
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseBlackHoleOrbit
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseStreamer
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAntiAFK
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAntiCrash
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseAutoGG
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseChatBypass
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseClientSpoofer
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseHypixelAutoPlay
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseInsults
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoGuiClose
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNoPitchLimit
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseNuker
import net.ccbluex.liquidbounce.features.module.modules.rise.RisePlayerNotifier
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseSpammer
import net.ccbluex.liquidbounce.features.module.modules.rise.RiseTimer
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
            Velocity2,
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
            AntiKnockBack,

            RiseComboOneHit,
            RiseCriticals,
            RiseVelocity,
            RiseTickBase,
            RiseTeleportAura,
            RiseThrowableAura,
            RiseWatchdogTPAura,
            RiseLegitReach,
            RisePiercing,
            RiseMimic,
            RiseFences,
            RiseKeepRange,
            RiseKnockbackSample,
            RiseLagBreak,
            RiseRegen,
            RiseRotationSnapshot,
            RiseAntiBot,
            RiseFlight,
            RiseSpeed,
            RiseLongJump,
            RiseNoSlow,
            RiseStep,
            RiseStrafe,
            RiseTargetStrafe,
            RiseInventoryMove,
            RiseNoClip,
            RisePhase,
            RiseSneak,
            RiseSprint,
            RiseSnapTap,
            RiseWallClimb,
            RiseJesus,
            RiseClipper,
            RiseAutoStuck,
            RiseStuck,
            RisePotionExtender,
            RiseTerrainSpeed,
            RiseTeleport,
            RiseAutoMLG,
            RiseNoJumpDelay,
            RiseResourcePackSpoof,
            RiseDisabler,
            RiseCrasher,
            RisePingSpoof,
            RiseNoRotate,
            RiseGhostHand,
            RiseGodMode,
            RiseConsoleSpammer,
            RiseBlockTracker,
            RiseLightningTracker,
            RiseStaffDetector,
            RiseAimAssist,
            RiseAimBacktrack,
            RiseAutoClicker,
            RiseClickAssist,
            RiseDisplacementSample,
            RiseFastPlace,
            RiseGuiClicker,
            RiseKeepSprint,
            RiseLegitScaffold,
            RiseManualKBDisplacement,
            RiseNoClickDelay,
            RiseOldWTap,
            RiseWTap,
            RiseESP,
            RiseNameTags,
            RiseTracers,
            RiseFullBright,
            RiseChestESP,
            RiseFreeCam,
            RiseFreeLook,
            RiseBreadCrumbs,
            RiseHurtCamera,
            RiseKillEffect,
            RiseTargetInfo,
            RiseSessionStats,
            RiseUnlimitedChat,
            RiseViewBobbing,
            RiseNoCameraClip,
            RiseOreESP,
            RiseM2DESP,
            RiseJumpCircles,
            RiseParticles,
            RiseAmbience,
            RiseBedPlates,
            RiseBlackHoleOrbit,
            RiseStreamer,
            RiseAntiAFK,
            RiseAntiCrash,
            RiseAutoGG,
            RiseChatBypass,
            RiseClientSpoofer,
            RiseHypixelAutoPlay,
            RiseInsults,
            RiseNoGuiClose,
            RiseNoPitchLimit,
            RiseNuker,
            RisePlayerNotifier,
            RiseSpammer,
            RiseTimer
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
