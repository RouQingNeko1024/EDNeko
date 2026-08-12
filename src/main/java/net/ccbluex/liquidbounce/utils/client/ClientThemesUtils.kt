/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.mixColors
import java.awt.Color

object ClientThemesUtils {

    var ClientColorMode: String = "MoonPurple"
        set(value) {
            field = value.lowercase()
        }

    var ThemeFadeSpeed: Int = 7
        set(value) {
            field = value.coerceIn(1, 10)
        }

    var updown: Boolean = false

    var BackgroundMode: String = "Synced"
        set(value) {
            field = value.lowercase()
        }

    var customBgColor: Color = Color(32, 32, 64)
    var neverLoseBgColor: Color = Color(60, 60, 60)

    private val themeColorMap: Map<String, Pair<Color, Color>> = mapOf(
        "zywl"      to Pair(Color(206, 58, 98),   Color(215, 171, 168)),
        "water"     to Pair(Color(35, 69, 148),   Color(108, 170, 207)),
        "magic"     to Pair(Color(255, 180, 255), Color(181, 139, 194)),
        "tree"      to Pair(Color(18, 155, 38),   Color(76, 255, 102)),
        "darknight" to Pair(Color(93, 95, 95),    Color(203, 200, 204)),
        "sun"       to Pair(Color(255, 143, 0),   Color(252, 205, 44)),
        "flower"    to Pair(Color(184, 85, 199),  Color(182, 140, 195)),
        "loyoi"     to Pair(Color(255, 131, 0),   Color(255, 131, 124)),
        "fdp"       to Pair(Color(255, 100, 255), Color(100, 255, 255)),
        "may"       to Pair(Color(255, 80, 255),  Color(255, 255, 255)),
        "mint"      to Pair(Color(85, 255, 140),  Color(85, 255, 255)),
        "cero"      to Pair(Color(170, 0, 170),   Color(170, 255, 170)),
        "azure"     to Pair(Color(0, 90, 255),    Color(0, 180, 255)),
        "pumpkin"   to Pair(Color(255, 216, 169), Color(241, 166, 98)),
        "polarized" to Pair(Color(0, 32, 64),     Color(173, 239, 209)),
        "sundae"    to Pair(Color(28, 28, 27),    Color(206, 74, 126)),
        "terminal"  to Pair(Color(25, 30, 25),    Color(15, 155, 15)),
        "coral"     to Pair(Color(52, 133, 151),  Color(244, 168, 150)),
        "fire"      to Pair(Color(255, 45, 30),   Color(255, 123, 15)),
        "aqua"      to Pair(Color(80, 255, 255),  Color(80, 190, 255)),
        "peony"     to Pair(Color(255, 120, 255), Color(255, 190, 255)),
        "vergren"   to Pair(Color(170, 255, 169), Color(17, 255, 189)),
        "eveningsunshine" to Pair(Color(185, 43, 39), Color(21, 101, 192)),
        "lightorange"     to Pair(Color(255, 183, 94), Color(237, 143, 3)),
        "reef"             to Pair(Color(0, 210, 255), Color(58, 123, 213)),
        "amin"             to Pair(Color(142, 45, 226), Color(74, 0, 224)),
        "magics"           to Pair(Color(89, 193, 115), Color(93, 38, 193)),
        "mangopulp"        to Pair(Color(240, 152, 25), Color(237, 222, 93)),
        "moonpurple"       to Pair(Color(78, 84, 200), Color(143, 148, 251)),
        "aqualicious"      to Pair(Color(80, 201, 195), Color(150, 222, 218)),
        "stripe"           to Pair(Color(31, 162, 255), Color(166, 255, 203)),
        "shifter"          to Pair(Color(188, 78, 156), Color(248, 7, 89)),
        "quepal"           to Pair(Color(17, 153, 142), Color(56, 239, 125)),
        "orca"             to Pair(Color(68, 160, 141), Color(9, 54, 55)),
        "sublimevivid"     to Pair(Color(252, 70, 107), Color(63, 94, 251)),
        "moonasteroid"     to Pair(Color(15, 32, 39), Color(44, 83, 100)),
        "summerdog"        to Pair(Color(168, 255, 120), Color(120, 255, 214)),
        "pinkflavour"      to Pair(Color(128, 0, 128), Color(255, 192, 203)),
        "sincityred"       to Pair(Color(237, 33, 58), Color(147, 41, 30)),
        "timber"           to Pair(Color(252, 0, 255), Color(0, 219, 222)),
        "pinotnoir"        to Pair(Color(75, 108, 183), Color(24, 40, 72)),
        "dirtyfog"         to Pair(Color(185, 147, 214), Color(140, 166, 219)),
        "piglet"           to Pair(Color(238, 156, 167), Color(255, 221, 225)),
        "littleleaf"       to Pair(Color(118, 184, 82), Color(141, 194, 111)),
        "nelson"           to Pair(Color(242, 112, 156), Color(255, 148, 114)),
        "turquoiseflow"    to Pair(Color(19, 106, 138), Color(38, 120, 113)),
        "purplin"          to Pair(Color(106, 48, 147), Color(160, 68, 255)),
        "martini"          to Pair(Color(253, 252, 71), Color(36, 254, 65)),
        "soundcloud"       to Pair(Color(254, 140, 0), Color(248, 54, 0)),
        "inbox"            to Pair(Color(69, 127, 202), Color(86, 145, 200)),
        "amethyst"         to Pair(Color(157, 80, 187), Color(110, 72, 170)),
        "blush"            to Pair(Color(178, 69, 146), Color(241, 95, 121)),
        "mocharose"        to Pair(Color(245, 194, 231), Color(243, 139, 168)),
        "neoncrimson"      to Pair(Color(10, 0, 15), Color(255, 20, 80)),
        "acidgreen"        to Pair(Color(15, 15, 15), Color(57, 255, 20)),
        "vaporwave"        to Pair(Color(20, 0, 40), Color(255, 100, 200)),
        "noir"             to Pair(Color(12, 12, 12), Color(230, 230, 230)),
        "obsidian"         to Pair(Color(5, 5, 5), Color(40, 40, 45)),
        "champagne"        to Pair(Color(60, 20, 25), Color(245, 215, 160)),
        "rosegold"         to Pair(Color(45, 20, 35), Color(220, 170, 160)),
        "arctic"           to Pair(Color(200, 220, 235), Color(240, 248, 255)),
        "frost"            to Pair(Color(180, 210, 230), Color(230, 245, 255)),
        "glacier"          to Pair(Color(30, 60, 90), Color(140, 200, 230)),
        "slate"            to Pair(Color(40, 42, 54), Color(98, 114, 164)),
        "abyss"            to Pair(Color(0, 8, 20), Color(15, 80, 130)),
        "biolum"           to Pair(Color(0, 15, 20), Color(0, 255, 180)),
        "evergreen"        to Pair(Color(10, 30, 15), Color(80, 200, 100)),
        "dusk"             to Pair(Color(40, 15, 50), Color(220, 100, 50)),
        "aurora"           to Pair(Color(10, 15, 40), Color(80, 255, 180)),
        "retrowave"        to Pair(Color(30, 0, 50), Color(255, 0, 110)),
        "y2k"              to Pair(Color(180, 120, 255), Color(255, 200, 230)),
        "dustyrose"        to Pair(Color(180, 140, 150), Color(220, 190, 195)),
        "sage"             to Pair(Color(140, 160, 140), Color(190, 210, 180)),
        "cloudburst"       to Pair(Color(150, 160, 180), Color(200, 210, 220)),
        "monolith"         to Pair(Color(0, 0, 0), Color(255, 255, 255)),
        "bloodline"        to Pair(Color(40, 0, 0), Color(200, 20, 20)),
        "lavender"         to Pair(Color(200, 180, 220), Color(230, 220, 240)),
        "butter"           to Pair(Color(240, 220, 170), Color(255, 245, 200)),
        "gothic"           to Pair(Color(8, 8, 12), Color(60, 10, 20)),
        "phantom"          to Pair(Color(20, 25, 40), Color(100, 120, 180)),
        "quicksilver"      to Pair(Color(180, 190, 200), Color(220, 230, 240)),
        "mercury"          to Pair(Color(40, 30, 60), Color(190, 200, 210)),
        "tropical"         to Pair(Color(0, 80, 100), Color(255, 220, 50)),
        "mango"            to Pair(Color(100, 30, 60), Color(255, 150, 50)),
        "rust"             to Pair(Color(60, 30, 20), Color(180, 80, 30)),
        "concrete"         to Pair(Color(90, 90, 90), Color(130, 130, 130)),
        "nebula"           to Pair(Color(5, 0, 15), Color(120, 50, 180)),
        "supernova"        to Pair(Color(10, 10, 30), Color(100, 150, 255)),
        "eclipse"          to Pair(Color(15, 5, 25), Color(80, 0, 120)),
        "iceberg"          to Pair(Color(150, 200, 220), Color(220, 240, 250)),
        "scarlet"          to Pair(Color(80, 10, 20), Color(230, 50, 50)),
        "cyberpink"        to Pair(Color(20, 0, 30), Color(255, 50, 150)),
        "matrix"           to Pair(Color(0, 10, 0), Color(0, 180, 0)),
        "solarglare"       to Pair(Color(40, 30, 0), Color(255, 200, 50)),
        "emerald"          to Pair(Color(0, 80, 40), Color(80, 200, 120)),
        "sapphire"         to Pair(Color(0, 30, 80), Color(50, 100, 200)),
        "ruby"             to Pair(Color(80, 10, 20), Color(220, 30, 60)),
        "topaz"            to Pair(Color(100, 60, 0), Color(255, 180, 50)),
        "amethyst2"        to Pair(Color(60, 20, 100), Color(180, 100, 220)),
        "jade"             to Pair(Color(0, 80, 50), Color(100, 180, 120)),
        "opal"             to Pair(Color(150, 100, 150), Color(200, 180, 220)),
        "garnet"           to Pair(Color(60, 10, 20), Color(180, 40, 60)),
        "turquoise"        to Pair(Color(0, 80, 100), Color(50, 200, 220)),
        "citrine"          to Pair(Color(120, 80, 0), Color(255, 200, 50)),
        "peridot"          to Pair(Color(80, 100, 0), Color(200, 220, 50)),
        "aquamarine"       to Pair(Color(0, 100, 120), Color(100, 220, 200)),
        "tanzanite"        to Pair(Color(40, 20, 100), Color(120, 80, 200)),
        "morganite"        to Pair(Color(150, 100, 120), Color(255, 180, 200)),
        "kunzite"          to Pair(Color(150, 100, 150), Color(255, 180, 220)),
        "spinel"           to Pair(Color(80, 40, 100), Color(200, 120, 180)),
        "zircon"           to Pair(Color(60, 80, 120), Color(150, 180, 220)),
        "tourmaline"       to Pair(Color(0, 100, 80), Color(100, 200, 150)),
        "alexandrite"      to Pair(Color(80, 40, 100), Color(100, 150, 200)),
        "iolite"           to Pair(Color(40, 40, 100), Color(120, 100, 180)),
        "chrysoberyl"      to Pair(Color(100, 100, 0), Color(220, 200, 80)),
        "beryl"            to Pair(Color(0, 100, 80), Color(80, 200, 180)),
        "corundum"         to Pair(Color(80, 20, 40), Color(200, 80, 100)),
        "beryl2"           to Pair(Color(60, 120, 80), Color(120, 200, 140)),
        "quartz"           to Pair(Color(180, 180, 200), Color(220, 220, 240)),
        "moonstone"        to Pair(Color(180, 180, 200), Color(240, 240, 255)),
        "sunstone"         to Pair(Color(150, 80, 30), Color(255, 180, 100)),
        "labradorite"      to Pair(Color(40, 50, 70), Color(120, 140, 180)),
        "spectrolite"      to Pair(Color(30, 40, 60), Color(100, 120, 180)),
        "apatite"          to Pair(Color(0, 80, 120), Color(80, 180, 220)),
        "fluorite"         to Pair(Color(80, 60, 120), Color(180, 140, 220)),
        "calcite"          to Pair(Color(200, 200, 180), Color(255, 255, 230)),
        "sodalite"         to Pair(Color(20, 40, 100), Color(60, 100, 180)),
        "lapis"            to Pair(Color(20, 30, 100), Color(60, 80, 180)),
        "malachite"        to Pair(Color(0, 80, 40), Color(50, 180, 100)),
        "azurite"          to Pair(Color(0, 40, 100), Color(50, 100, 200)),
        "rhodochrosite"    to Pair(Color(150, 80, 100), Color(255, 150, 180)),
        "rhodonite"        to Pair(Color(100, 50, 60), Color(200, 120, 130)),
        "serpentine"       to Pair(Color(40, 80, 40), Color(100, 160, 80)),
        "serpentine2"      to Pair(Color(60, 100, 50), Color(120, 180, 100)),
        "howlite"          to Pair(Color(200, 200, 210), Color(240, 240, 250)),
        "obsidian2"        to Pair(Color(20, 15, 25), Color(60, 50, 70)),
        "onyx"             to Pair(Color(20, 20, 25), Color(80, 80, 90)),
        "jasper"           to Pair(Color(120, 60, 40), Color(200, 120, 80)),
        "agate"            to Pair(Color(150, 100, 120), Color(220, 180, 200)),
        "chert"            to Pair(Color(80, 80, 90), Color(150, 150, 160)),
        "flint"            to Pair(Color(50, 50, 60), Color(120, 120, 130)),
        "chert2"           to Pair(Color(60, 70, 80), Color(130, 140, 150)),
        "basalt"           to Pair(Color(40, 40, 50), Color(100, 100, 110)),
        "granite"          to Pair(Color(150, 120, 100), Color(220, 190, 170)),
        "marble"           to Pair(Color(200, 200, 210), Color(250, 250, 255)),
        "sandstone"        to Pair(Color(180, 150, 100), Color(240, 210, 160)),
        "limestone"        to Pair(Color(180, 180, 170), Color(240, 240, 230)),
        "shale"            to Pair(Color(60, 60, 70), Color(120, 120, 130)),
        "slate2"           to Pair(Color(50, 55, 65), Color(100, 110, 120)),
        "quartzite"        to Pair(Color(200, 200, 210), Color(255, 255, 255)),
        "gneiss"           to Pair(Color(100, 90, 80), Color(180, 170, 160)),
        "schist"           to Pair(Color(80, 80, 70), Color(150, 150, 140)),
        "diorite"          to Pair(Color(180, 180, 190), Color(240, 240, 250)),
        "andesite"         to Pair(Color(100, 100, 100), Color(170, 170, 170)),
        "rhyolite"         to Pair(Color(150, 130, 120), Color(220, 200, 190)),
        "pumice"           to Pair(Color(150, 140, 130), Color(220, 210, 200)),
        "scoria"           to Pair(Color(60, 40, 40), Color(120, 90, 90)),
        "tuff"             to Pair(Color(100, 100, 100), Color(160, 160, 160)),
        "dolomite"         to Pair(Color(180, 180, 170), Color(240, 240, 230)),
        "halite"           to Pair(Color(200, 220, 240), Color(255, 255, 255)),
        "gypsum"           to Pair(Color(200, 200, 190), Color(255, 255, 245)),
        "anhydrite"        to Pair(Color(180, 180, 200), Color(230, 230, 250)),
        "barite"           to Pair(Color(200, 200, 220), Color(255, 255, 255)),
        "celestine"        to Pair(Color(150, 180, 220), Color(200, 220, 255)),
        "anglesite"        to Pair(Color(200, 200, 180), Color(255, 255, 230)),
        "galena"           to Pair(Color(60, 60, 70), Color(120, 120, 130)),
        "sphalerite"       to Pair(Color(80, 60, 30), Color(180, 140, 80)),
        "cinnabar"         to Pair(Color(100, 20, 20), Color(220, 50, 50)),
        "stibnite"         to Pair(Color(50, 50, 60), Color(120, 120, 130)),
        "orpiment"         to Pair(Color(180, 140, 0), Color(255, 200, 50)),
        "realgar"          to Pair(Color(120, 40, 0), Color(220, 80, 20)),
        "copper"           to Pair(Color(120, 60, 30), Color(220, 130, 70)),
        "silver"           to Pair(Color(150, 150, 160), Color(220, 220, 230)),
        "gold"             to Pair(Color(120, 90, 0), Color(255, 200, 50)),
        "platinum"         to Pair(Color(160, 160, 170), Color(230, 230, 240)),
        "titanium"         to Pair(Color(140, 140, 150), Color(200, 200, 210)),
        "cobalt"           to Pair(Color(40, 50, 100), Color(80, 100, 180)),
        "nickel"           to Pair(Color(100, 110, 100), Color(180, 190, 180)),
        "chromium"         to Pair(Color(60, 80, 60), Color(120, 160, 120)),
        "manganese"        to Pair(Color(80, 60, 80), Color(160, 120, 160)),
        "vanadium"         to Pair(Color(60, 60, 80), Color(120, 120, 160)),
        "molybdenum"       to Pair(Color(70, 70, 80), Color(140, 140, 150)),
        "tungsten"         to Pair(Color(60, 60, 60), Color(130, 130, 130)),
        "ocean"            to Pair(Color(0, 50, 100), Color(0, 150, 200)),
        "sunset"           to Pair(Color(255, 100, 50), Color(255, 180, 100)),
        "forest"           to Pair(Color(20, 60, 30), Color(60, 140, 60)),
        "midnight"         to Pair(Color(10, 10, 40), Color(40, 40, 100)),
        "cherry"           to Pair(Color(200, 50, 80), Color(255, 150, 170)),
        "minty"            to Pair(Color(100, 200, 150), Color(180, 255, 200)),
        "coralreef"        to Pair(Color(255, 100, 120), Color(255, 180, 150)),
        "thunder"          to Pair(Color(50, 0, 100), Color(150, 100, 255)),
        "honey"            to Pair(Color(200, 150, 50), Color(255, 220, 100)),
        "ice"              to Pair(Color(150, 200, 255), Color(220, 240, 255)),
        "velvet"           to Pair(Color(100, 20, 40), Color(180, 60, 80)),
        "moss"             to Pair(Color(50, 70, 40), Color(100, 130, 70)),
        "plum"             to Pair(Color(80, 40, 100), Color(150, 80, 180)),
        "sand"             to Pair(Color(180, 150, 100), Color(230, 200, 150)),
        "storm"            to Pair(Color(50, 50, 70), Color(100, 100, 130)),
        "peach"            to Pair(Color(255, 180, 150), Color(255, 220, 200)),
        "denim"            to Pair(Color(50, 80, 140), Color(100, 140, 200)),
        "olive"            to Pair(Color(100, 100, 50), Color(150, 150, 80)),
        "wine"             to Pair(Color(80, 20, 40), Color(150, 50, 70)),
        "sky"              to Pair(Color(100, 180, 255), Color(180, 220, 255)),
        "amber"            to Pair(Color(200, 120, 50), Color(255, 180, 80)),
        "fern"             to Pair(Color(60, 120, 60), Color(120, 180, 100)),
        "iris"             to Pair(Color(100, 80, 160), Color(180, 140, 220)),
        "crimson"          to Pair(Color(150, 20, 40), Color(220, 50, 70)),
        "indigo"           to Pair(Color(50, 50, 150), Color(100, 100, 200)),
        "magenta"          to Pair(Color(180, 50, 150), Color(255, 100, 200)),
        "ochre"            to Pair(Color(180, 120, 50), Color(220, 160, 80)),
        "sienna"           to Pair(Color(150, 70, 40), Color(200, 110, 70)),
        "violet"           to Pair(Color(100, 50, 150), Color(180, 100, 220)),
        "chartreuse"       to Pair(Color(150, 200, 50), Color(200, 255, 80)),
        "fuchsia"          to Pair(Color(180, 50, 150), Color(255, 100, 200)),
        "lime"             to Pair(Color(100, 200, 50), Color(150, 255, 80)),
        "navy"             to Pair(Color(20, 30, 80), Color(50, 70, 140)),
        "salmon"           to Pair(Color(250, 128, 114), Color(255, 180, 170)),
        "tan"              to Pair(Color(180, 150, 120), Color(220, 190, 160)),
        "khaki"            to Pair(Color(180, 170, 100), Color(230, 220, 150)),
        "maroon"           to Pair(Color(100, 20, 40), Color(160, 50, 70)),
        "teal"             to Pair(Color(0, 128, 128), Color(80, 200, 200)),
        "cyan"             to Pair(Color(0, 180, 200), Color(100, 230, 255)),
        "bronze"           to Pair(Color(140, 100, 50), Color(200, 150, 80)),
        "pearl"            to Pair(Color(220, 210, 200), Color(250, 245, 240)),
        "mahogany"         to Pair(Color(120, 40, 40), Color(180, 80, 70)),
        "jade2"            to Pair(Color(0, 100, 80), Color(80, 180, 140)),
        "cobalt2"          to Pair(Color(30, 60, 120), Color(60, 100, 180)),
        "emerald2"         to Pair(Color(0, 120, 80), Color(80, 200, 140)),
        "sapphire2"        to Pair(Color(20, 50, 120), Color(60, 100, 200)),
        "ruby2"            to Pair(Color(120, 20, 40), Color(200, 50, 80)),
        "topaz2"           to Pair(Color(150, 100, 50), Color(230, 180, 100)),
        "amethyst3"        to Pair(Color(80, 40, 120), Color(160, 100, 200)),
        "aquamarine2"      to Pair(Color(50, 150, 140), Color(120, 220, 200)),
        "garnet2"          to Pair(Color(100, 30, 40), Color(180, 60, 70)),
        "opal2"            to Pair(Color(180, 150, 180), Color(230, 200, 230)),
        "tourmaline2"      to Pair(Color(30, 120, 100), Color(80, 200, 160)),
        "zircon2"          to Pair(Color(80, 100, 140), Color(140, 170, 210)),
        "peridot2"         to Pair(Color(100, 130, 50), Color(180, 220, 80)),
        "tanzanite2"       to Pair(Color(60, 40, 120), Color(120, 80, 200)),
        "spinel2"          to Pair(Color(100, 60, 120), Color(180, 120, 200)),
        "morganite2"       to Pair(Color(200, 140, 160), Color(255, 200, 210)),
        "kunzite2"         to Pair(Color(180, 130, 170), Color(250, 200, 230)),
        "alexandrite2"     to Pair(Color(100, 60, 120), Color(140, 120, 200)),
        "iolite2"          to Pair(Color(60, 60, 120), Color(120, 110, 180)),
        "chrysoberyl2"     to Pair(Color(130, 130, 50), Color(200, 200, 100)),
        "beryl3"           to Pair(Color(50, 130, 100), Color(100, 200, 160)),
        "corundum2"        to Pair(Color(100, 40, 60), Color(180, 80, 100)),
        "quartz2"          to Pair(Color(200, 200, 210), Color(240, 240, 250)),
        "moonstone2"       to Pair(Color(190, 190, 210), Color(245, 245, 255)),
        "sunstone2"        to Pair(Color(160, 90, 40), Color(255, 190, 110)),
        "labradorite2"     to Pair(Color(50, 60, 80), Color(130, 150, 190)),
        "spectrolite2"     to Pair(Color(40, 50, 70), Color(110, 130, 190)),
        "apatite2"         to Pair(Color(10, 90, 130), Color(90, 190, 230)),
        "fluorite2"        to Pair(Color(90, 70, 130), Color(190, 150, 230)),
        "calcite2"         to Pair(Color(210, 210, 190), Color(255, 255, 240)),
        "sodalite2"        to Pair(Color(30, 50, 110), Color(70, 110, 190)),
        "lapis2"           to Pair(Color(30, 40, 110), Color(70, 90, 190)),
        "malachite2"       to Pair(Color(10, 90, 50), Color(60, 190, 110)),
        "azurite2"         to Pair(Color(10, 50, 110), Color(60, 110, 210)),
        "rhodochrosite2"   to Pair(Color(160, 90, 110), Color(255, 160, 190)),
        "rhodonite2"       to Pair(Color(110, 60, 70), Color(210, 130, 140)),
        "howlite2"         to Pair(Color(210, 210, 220), Color(250, 250, 255)),
        "jasper2"          to Pair(Color(130, 70, 50), Color(210, 130, 90)),
        "agate2"           to Pair(Color(160, 110, 130), Color(230, 190, 210)),
        "celestine2"       to Pair(Color(160, 190, 230), Color(210, 230, 255)),
        "anglesite2"       to Pair(Color(210, 210, 190), Color(255, 255, 240)),
        "galena2"          to Pair(Color(70, 70, 80), Color(130, 130, 140)),
        "sphalerite2"      to Pair(Color(90, 70, 40), Color(190, 150, 90)),
        "cinnabar2"        to Pair(Color(110, 30, 30), Color(230, 60, 60)),
        "stibnite2"        to Pair(Color(60, 60, 70), Color(130, 130, 140)),
        "orpiment2"        to Pair(Color(190, 150, 10), Color(255, 210, 60)),
        "realgar2"         to Pair(Color(130, 50, 10), Color(230, 90, 30)),
        "copper2"          to Pair(Color(130, 70, 40), Color(230, 140, 80)),
        "silver2"          to Pair(Color(160, 160, 170), Color(230, 230, 240)),
        "gold2"            to Pair(Color(130, 100, 10), Color(255, 210, 60)),
        "platinum2"        to Pair(Color(170, 170, 180), Color(240, 240, 250)),
        "titanium2"        to Pair(Color(150, 150, 160), Color(210, 210, 220)),
        "cobalt3"          to Pair(Color(50, 60, 110), Color(90, 110, 190)),
        "nickel2"          to Pair(Color(110, 120, 110), Color(190, 200, 190)),
        "chromium2"        to Pair(Color(70, 90, 70), Color(130, 170, 130)),
        "manganese2"       to Pair(Color(90, 70, 90), Color(170, 130, 170)),
        "vanadium2"        to Pair(Color(70, 70, 90), Color(130, 130, 170)),
        "molybdenum2"      to Pair(Color(80, 80, 90), Color(150, 150, 160)),
        "tungsten2"        to Pair(Color(70, 70, 70), Color(140, 140, 140))
    )

    private fun parseHexColor(hexString: String): Color {
        val raw = hexString.replace("#", "")
        return try {
            val colorLong = raw.toLong(16)
            when (raw.length) {
                6 -> Color(colorLong.toInt() and 0xFFFFFF)
                8 -> Color((colorLong and 0xFFFFFFFF).toInt(), true)
                else -> Color(-1)
            }
        } catch (e: NumberFormatException) {
            Color(-1)
        }
    }

    fun getBackgroundColor(index: Int = 0, alpha: Int = 255): Color {
        val m = BackgroundMode.lowercase()

        if (m.startsWith("#")) {
            return parseHexColor(m).let { Color(it.red, it.green, it.blue, alpha) }
        }

        return when (m) {
            "dark" -> Color(21, 21, 21, alpha)
            "synced" -> getColorWithAlpha(index, alpha).darker().darker()
            "custom" -> Color(customBgColor.red, customBgColor.green, customBgColor.blue, alpha)
            "neverlose" -> Color(neverLoseBgColor.red, neverLoseBgColor.green, neverLoseBgColor.blue, alpha)
            "none" -> Color(0, 0, 0, 0)
            else -> Color(21, 21, 21, alpha)
        }
    }

    fun setColor(type: String, alpha: Int): Color {
        val mode = ClientColorMode.lowercase()
        if (mode.startsWith("#")) {
            return parseHexColor(mode).let { Color(it.red, it.green, it.blue, alpha) }
        }
        return when (mode) {
            "zywl" ->
                if (type == "start") Color(215, 171, 168, alpha) else Color(206, 58, 98, alpha)
            "water" ->
                if (type == "start") Color(108, 170, 207, alpha) else Color(35, 69, 148, alpha)
            "magic" ->
                if (type == "start") Color(255, 180, 255, alpha) else Color(192, 67, 255, alpha)
            "darknight" ->
                if (type == "start") Color(203, 200, 204, alpha) else Color(93, 95, 95, alpha)
            "sun" ->
                if (type == "start") Color(252, 205, 44, alpha) else Color(255, 143, 0, alpha)
            "flower" ->
                if (type == "start") Color(182, 140, 195, alpha) else Color(184, 85, 199, alpha)
            "loyoi" ->
                if (type == "start") Color(255, 131, 124, alpha) else Color(255, 131, 0, alpha)
            "fdp" ->
                if (type == "start") Color(100, 255, 255, alpha) else Color(255, 100, 255, alpha)
            "may" ->
                if (type == "start") Color(255, 255, 255, alpha) else Color(255, 80, 255, alpha)
            "mint" ->
                if (type == "start") Color(85, 255, 255, alpha) else Color(85, 255, 140, alpha)
            "cero" ->
                if (type == "start") Color(170, 255, 170, alpha) else Color(170, 0, 170, alpha)
            "azure" ->
                if (type == "start") Color(0, 180, 255, alpha) else Color(0, 90, 255, alpha)
            "pumpkin" ->
                if (type == "start") Color(241, 166, 98, alpha) else Color(255, 216, 169, alpha)
            "polarized" ->
                if (type == "start") Color(173, 239, 209, alpha) else Color(0, 32, 64, alpha)
            "sundae" ->
                if (type == "start") Color(206, 74, 126, alpha) else Color(28, 28, 27, alpha)
            "terminal" ->
                if (type == "start") Color(15, 155, 15, alpha) else Color(25, 30, 25, alpha)
            "coral" ->
                if (type == "start") Color(244, 168, 150, alpha) else Color(52, 133, 151, alpha)
            "fire" ->
                if (type == "start") Color(255, 45, 30, alpha) else Color(255, 123, 15, alpha)
            "aqua" ->
                if (type == "start") Color(80, 255, 255, alpha) else Color(80, 190, 255, alpha)
            "peony" ->
                if (type == "start") Color(255, 120, 255, alpha) else Color(255, 190, 255, alpha)
            "vergren" ->
                if (type == "start") Color(170, 255, 169, alpha) else Color(17, 255, 189, alpha)
            "eveningsunshine" ->
                if (type == "start") Color(185, 43, 39, alpha) else Color(21, 101, 192, alpha)
            "lightorange" ->
                if (type == "start") Color(255, 183, 94, alpha) else Color(237, 143, 3, alpha)
            "reef" ->
                if (type == "start") Color(0, 210, 255, alpha) else Color(58, 123, 213, alpha)
            "amin" ->
                if (type == "start") Color(142, 45, 226, alpha) else Color(74, 0, 224, alpha)
            "magics" ->
                if (type == "start") Color(89, 193, 115, alpha) else Color(93, 38, 193, alpha)
            "mangopulp" ->
                if (type == "start") Color(240, 152, 25, alpha) else Color(237, 222, 93, alpha)
            "moonpurple" ->
                if (type == "start") Color(78, 84, 200, alpha) else Color(143, 148, 251, alpha)
            "aqualicious" ->
                if (type == "start") Color(80, 201, 195, alpha) else Color(150, 222, 218, alpha)
            "stripe" ->
                if (type == "start") Color(31, 162, 255, alpha) else Color(166, 255, 203, alpha)
            "shifter" ->
                if (type == "start") Color(188, 78, 156, alpha) else Color(248, 7, 89, alpha)
            "quepal" ->
                if (type == "start") Color(17, 153, 142, alpha) else Color(56, 239, 125, alpha)
            "orca" ->
                if (type == "start") Color(68, 160, 141, alpha) else Color(9, 54, 55, alpha)
            "sublimevivid" ->
                if (type == "start") Color(252, 70, 107, alpha) else Color(63, 94, 251, alpha)
            "moonasteroid" ->
                if (type == "start") Color(15, 32, 39, alpha) else Color(44, 83, 100, alpha)
            "summerdog" ->
                if (type == "start") Color(168, 255, 120, alpha) else Color(120, 255, 214, alpha)
            "pinkflavour" ->
                if (type == "start") Color(128, 0, 128, alpha) else Color(255, 192, 203, alpha)
            "sincityred" ->
                if (type == "start") Color(237, 33, 58, alpha) else Color(147, 41, 30, alpha)
            "timber" ->
                if (type == "start") Color(252, 0, 255, alpha) else Color(0, 219, 222, alpha)
            "pinotnoir" ->
                if (type == "start") Color(75, 108, 183, alpha) else Color(24, 40, 72, alpha)
            "dirtyfog" ->
                if (type == "start") Color(185, 147, 214, alpha) else Color(140, 166, 219, alpha)
            "piglet" ->
                if (type == "start") Color(238, 156, 167, alpha) else Color(255, 221, 225, alpha)
            "littleleaf" ->
                if (type == "start") Color(118, 184, 82, alpha) else Color(141, 194, 111, alpha)
            "nelson" ->
                if (type == "start") Color(242, 112, 156, alpha) else Color(255, 148, 114, alpha)
            "turquoiseflow" ->
                if (type == "start") Color(19, 106, 138, alpha) else Color(38, 120, 113, alpha)
            "purplin" ->
                if (type == "start") Color(106, 48, 147, alpha) else Color(160, 68, 255, alpha)
            "martini" ->
                if (type == "start") Color(253, 252, 71, alpha) else Color(36, 254, 65, alpha)
            "soundcloud" ->
                if (type == "start") Color(254, 140, 0, alpha) else Color(248, 54, 0, alpha)
            "inbox" ->
                if (type == "start") Color(69, 127, 202, alpha) else Color(86, 145, 200, alpha)
            "amethyst" ->
                if (type == "start") Color(157, 80, 187, alpha) else Color(110, 72, 170, alpha)
            "blush" ->
                if (type == "start") Color(178, 69, 146, alpha) else Color(241, 95, 121, alpha)
            "mocharose" ->
                if (type == "start") Color(245, 194, 231, alpha) else Color(243, 139, 168, alpha)
            "neoncrimson" ->
                if (type == "start") Color(10, 0, 15, alpha) else Color(255, 20, 80, alpha)
            "acidgreen" ->
                if (type == "start") Color(15, 15, 15, alpha) else Color(57, 255, 20, alpha)
            "vaporwave" ->
                if (type == "start") Color(20, 0, 40, alpha) else Color(255, 100, 200, alpha)
            "noir" ->
                if (type == "start") Color(12, 12, 12, alpha) else Color(230, 230, 230, alpha)
            "obsidian" ->
                if (type == "start") Color(5, 5, 5, alpha) else Color(40, 40, 45, alpha)
            "champagne" ->
                if (type == "start") Color(60, 20, 25, alpha) else Color(245, 215, 160, alpha)
            "rosegold" ->
                if (type == "start") Color(45, 20, 35, alpha) else Color(220, 170, 160, alpha)
            "arctic" ->
                if (type == "start") Color(200, 220, 235, alpha) else Color(240, 248, 255, alpha)
            "frost" ->
                if (type == "start") Color(180, 210, 230, alpha) else Color(230, 245, 255, alpha)
            "glacier" ->
                if (type == "start") Color(30, 60, 90, alpha) else Color(140, 200, 230, alpha)
            "slate" ->
                if (type == "start") Color(40, 42, 54, alpha) else Color(98, 114, 164, alpha)
            "abyss" ->
                if (type == "start") Color(0, 8, 20, alpha) else Color(15, 80, 130, alpha)
            "biolum" ->
                if (type == "start") Color(0, 15, 20, alpha) else Color(0, 255, 180, alpha)
            "evergreen" ->
                if (type == "start") Color(10, 30, 15, alpha) else Color(80, 200, 100, alpha)
            "dusk" ->
                if (type == "start") Color(40, 15, 50, alpha) else Color(220, 100, 50, alpha)
            "aurora" ->
                if (type == "start") Color(10, 15, 40, alpha) else Color(80, 255, 180, alpha)
            "retrowave" ->
                if (type == "start") Color(30, 0, 50, alpha) else Color(255, 0, 110, alpha)
            "y2k" ->
                if (type == "start") Color(180, 120, 255, alpha) else Color(255, 200, 230, alpha)
            "dustyrose" ->
                if (type == "start") Color(180, 140, 150, alpha) else Color(220, 190, 195, alpha)
            "sage" ->
                if (type == "start") Color(140, 160, 140, alpha) else Color(190, 210, 180, alpha)
            "cloudburst" ->
                if (type == "start") Color(150, 160, 180, alpha) else Color(200, 210, 220, alpha)
            "monolith" ->
                if (type == "start") Color(0, 0, 0, alpha) else Color(255, 255, 255, alpha)
            "bloodline" ->
                if (type == "start") Color(40, 0, 0, alpha) else Color(200, 20, 20, alpha)
            "lavender" ->
                if (type == "start") Color(200, 180, 220, alpha) else Color(230, 220, 240, alpha)
            "butter" ->
                if (type == "start") Color(240, 220, 170, alpha) else Color(255, 245, 200, alpha)
            "gothic" ->
                if (type == "start") Color(8, 8, 12, alpha) else Color(60, 10, 20, alpha)
            "phantom" ->
                if (type == "start") Color(20, 25, 40, alpha) else Color(100, 120, 180, alpha)
            "quicksilver" ->
                if (type == "start") Color(180, 190, 200, alpha) else Color(220, 230, 240, alpha)
            "mercury" ->
                if (type == "start") Color(40, 30, 60, alpha) else Color(190, 200, 210, alpha)
            "tropical" ->
                if (type == "start") Color(0, 80, 100, alpha) else Color(255, 220, 50, alpha)
            "mango" ->
                if (type == "start") Color(100, 30, 60, alpha) else Color(255, 150, 50, alpha)
            "rust" ->
                if (type == "start") Color(60, 30, 20, alpha) else Color(180, 80, 30, alpha)
            "concrete" ->
                if (type == "start") Color(90, 90, 90, alpha) else Color(130, 130, 130, alpha)
            "nebula" ->
                if (type == "start") Color(5, 0, 15, alpha) else Color(120, 50, 180, alpha)
            "supernova" ->
                if (type == "start") Color(10, 10, 30, alpha) else Color(100, 150, 255, alpha)
            "eclipse" ->
                if (type == "start") Color(15, 5, 25, alpha) else Color(80, 0, 120, alpha)
            "iceberg" ->
                if (type == "start") Color(150, 200, 220, alpha) else Color(220, 240, 250, alpha)
            "scarlet" ->
                if (type == "start") Color(80, 10, 20, alpha) else Color(230, 50, 50, alpha)
            "cyberpink" ->
                if (type == "start") Color(20, 0, 30, alpha) else Color(255, 50, 150, alpha)
            "matrix" ->
                if (type == "start") Color(0, 10, 0, alpha) else Color(0, 180, 0, alpha)
            "solarglare" ->
                if (type == "start") Color(40, 30, 0, alpha) else Color(255, 200, 50, alpha)
            "emerald" ->
                if (type == "start") Color(0, 80, 40, alpha) else Color(80, 200, 120, alpha)
            "sapphire" ->
                if (type == "start") Color(0, 30, 80, alpha) else Color(50, 100, 200, alpha)
            "ruby" ->
                if (type == "start") Color(80, 10, 20, alpha) else Color(220, 30, 60, alpha)
            "topaz" ->
                if (type == "start") Color(100, 60, 0, alpha) else Color(255, 180, 50, alpha)
            "amethyst2" ->
                if (type == "start") Color(60, 20, 100, alpha) else Color(180, 100, 220, alpha)
            "jade" ->
                if (type == "start") Color(0, 80, 50, alpha) else Color(100, 180, 120, alpha)
            "opal" ->
                if (type == "start") Color(150, 100, 150, alpha) else Color(200, 180, 220, alpha)
            "garnet" ->
                if (type == "start") Color(60, 10, 20, alpha) else Color(180, 40, 60, alpha)
            "turquoise" ->
                if (type == "start") Color(0, 80, 100, alpha) else Color(50, 200, 220, alpha)
            "citrine" ->
                if (type == "start") Color(120, 80, 0, alpha) else Color(255, 200, 50, alpha)
            "peridot" ->
                if (type == "start") Color(80, 100, 0, alpha) else Color(200, 220, 50, alpha)
            "aquamarine" ->
                if (type == "start") Color(0, 100, 120, alpha) else Color(100, 220, 200, alpha)
            "tanzanite" ->
                if (type == "start") Color(40, 20, 100, alpha) else Color(120, 80, 200, alpha)
            "morganite" ->
                if (type == "start") Color(150, 100, 120, alpha) else Color(255, 180, 200, alpha)
            "kunzite" ->
                if (type == "start") Color(150, 100, 150, alpha) else Color(255, 180, 220, alpha)
            "spinel" ->
                if (type == "start") Color(80, 40, 100, alpha) else Color(200, 120, 180, alpha)
            "zircon" ->
                if (type == "start") Color(60, 80, 120, alpha) else Color(150, 180, 220, alpha)
            "tourmaline" ->
                if (type == "start") Color(0, 100, 80, alpha) else Color(100, 200, 150, alpha)
            "alexandrite" ->
                if (type == "start") Color(80, 40, 100, alpha) else Color(100, 150, 200, alpha)
            "iolite" ->
                if (type == "start") Color(40, 40, 100, alpha) else Color(120, 100, 180, alpha)
            "chrysoberyl" ->
                if (type == "start") Color(100, 100, 0, alpha) else Color(220, 200, 80, alpha)
            "beryl" ->
                if (type == "start") Color(0, 100, 80, alpha) else Color(80, 200, 180, alpha)
            "corundum" ->
                if (type == "start") Color(80, 20, 40, alpha) else Color(200, 80, 100, alpha)
            "beryl2" ->
                if (type == "start") Color(60, 120, 80, alpha) else Color(120, 200, 140, alpha)
            "quartz" ->
                if (type == "start") Color(180, 180, 200, alpha) else Color(220, 220, 240, alpha)
            "moonstone" ->
                if (type == "start") Color(180, 180, 200, alpha) else Color(240, 240, 255, alpha)
            "sunstone" ->
                if (type == "start") Color(150, 80, 30, alpha) else Color(255, 180, 100, alpha)
            "labradorite" ->
                if (type == "start") Color(40, 50, 70, alpha) else Color(120, 140, 180, alpha)
            "spectrolite" ->
                if (type == "start") Color(30, 40, 60, alpha) else Color(100, 120, 180, alpha)
            "apatite" ->
                if (type == "start") Color(0, 80, 120, alpha) else Color(80, 180, 220, alpha)
            "fluorite" ->
                if (type == "start") Color(80, 60, 120, alpha) else Color(180, 140, 220, alpha)
            "calcite" ->
                if (type == "start") Color(200, 200, 180, alpha) else Color(255, 255, 230, alpha)
            "sodalite" ->
                if (type == "start") Color(20, 40, 100, alpha) else Color(60, 100, 180, alpha)
            "lapis" ->
                if (type == "start") Color(20, 30, 100, alpha) else Color(60, 80, 180, alpha)
            "malachite" ->
                if (type == "start") Color(0, 80, 40, alpha) else Color(50, 180, 100, alpha)
            "azurite" ->
                if (type == "start") Color(0, 40, 100, alpha) else Color(50, 100, 200, alpha)
            "rhodochrosite" ->
                if (type == "start") Color(150, 80, 100, alpha) else Color(255, 150, 180, alpha)
            "rhodonite" ->
                if (type == "start") Color(100, 50, 60, alpha) else Color(200, 120, 130, alpha)
            "serpentine" ->
                if (type == "start") Color(40, 80, 40, alpha) else Color(100, 160, 80, alpha)
            "serpentine2" ->
                if (type == "start") Color(60, 100, 50, alpha) else Color(120, 180, 100, alpha)
            "howlite" ->
                if (type == "start") Color(200, 200, 210, alpha) else Color(240, 240, 250, alpha)
            "obsidian2" ->
                if (type == "start") Color(20, 15, 25, alpha) else Color(60, 50, 70, alpha)
            "onyx" ->
                if (type == "start") Color(20, 20, 25, alpha) else Color(80, 80, 90, alpha)
            "jasper" ->
                if (type == "start") Color(120, 60, 40, alpha) else Color(200, 120, 80, alpha)
            "agate" ->
                if (type == "start") Color(150, 100, 120, alpha) else Color(220, 180, 200, alpha)
            "chert" ->
                if (type == "start") Color(80, 80, 90, alpha) else Color(150, 150, 160, alpha)
            "flint" ->
                if (type == "start") Color(50, 50, 60, alpha) else Color(120, 120, 130, alpha)
            "chert2" ->
                if (type == "start") Color(60, 70, 80, alpha) else Color(130, 140, 150, alpha)
            "basalt" ->
                if (type == "start") Color(40, 40, 50, alpha) else Color(100, 100, 110, alpha)
            "granite" ->
                if (type == "start") Color(150, 120, 100, alpha) else Color(220, 190, 170, alpha)
            "marble" ->
                if (type == "start") Color(200, 200, 210, alpha) else Color(250, 250, 255, alpha)
            "sandstone" ->
                if (type == "start") Color(180, 150, 100, alpha) else Color(240, 210, 160, alpha)
            "limestone" ->
                if (type == "start") Color(180, 180, 170, alpha) else Color(240, 240, 230, alpha)
            "shale" ->
                if (type == "start") Color(60, 60, 70, alpha) else Color(120, 120, 130, alpha)
            "slate2" ->
                if (type == "start") Color(50, 55, 65, alpha) else Color(100, 110, 120, alpha)
            "quartzite" ->
                if (type == "start") Color(200, 200, 210, alpha) else Color(255, 255, 255, alpha)
            "gneiss" ->
                if (type == "start") Color(100, 90, 80, alpha) else Color(180, 170, 160, alpha)
            "schist" ->
                if (type == "start") Color(80, 80, 70, alpha) else Color(150, 150, 140, alpha)
            "diorite" ->
                if (type == "start") Color(180, 180, 190, alpha) else Color(240, 240, 250, alpha)
            "andesite" ->
                if (type == "start") Color(100, 100, 100, alpha) else Color(170, 170, 170, alpha)
            "rhyolite" ->
                if (type == "start") Color(150, 130, 120, alpha) else Color(220, 200, 190, alpha)
            "pumice" ->
                if (type == "start") Color(150, 140, 130, alpha) else Color(220, 210, 200, alpha)
            "scoria" ->
                if (type == "start") Color(60, 40, 40, alpha) else Color(120, 90, 90, alpha)
            "tuff" ->
                if (type == "start") Color(100, 100, 100, alpha) else Color(160, 160, 160, alpha)
            "dolomite" ->
                if (type == "start") Color(180, 180, 170, alpha) else Color(240, 240, 230, alpha)
            "halite" ->
                if (type == "start") Color(200, 220, 240, alpha) else Color(255, 255, 255, alpha)
            "gypsum" ->
                if (type == "start") Color(200, 200, 190, alpha) else Color(255, 255, 245, alpha)
            "anhydrite" ->
                if (type == "start") Color(180, 180, 200, alpha) else Color(230, 230, 250, alpha)
            "barite" ->
                if (type == "start") Color(200, 200, 220, alpha) else Color(255, 255, 255, alpha)
            "celestine" ->
                if (type == "start") Color(150, 180, 220, alpha) else Color(200, 220, 255, alpha)
            "anglesite" ->
                if (type == "start") Color(200, 200, 180, alpha) else Color(255, 255, 230, alpha)
            "galena" ->
                if (type == "start") Color(60, 60, 70, alpha) else Color(120, 120, 130, alpha)
            "sphalerite" ->
                if (type == "start") Color(80, 60, 30, alpha) else Color(180, 140, 80, alpha)
            "cinnabar" ->
                if (type == "start") Color(100, 20, 20, alpha) else Color(220, 50, 50, alpha)
            "stibnite" ->
                if (type == "start") Color(50, 50, 60, alpha) else Color(120, 120, 130, alpha)
            "orpiment" ->
                if (type == "start") Color(180, 140, 0, alpha) else Color(255, 200, 50, alpha)
            "realgar" ->
                if (type == "start") Color(120, 40, 0, alpha) else Color(220, 80, 20, alpha)
            "copper" ->
                if (type == "start") Color(120, 60, 30, alpha) else Color(220, 130, 70, alpha)
            "silver" ->
                if (type == "start") Color(150, 150, 160, alpha) else Color(220, 220, 230, alpha)
            "gold" ->
                if (type == "start") Color(120, 90, 0, alpha) else Color(255, 200, 50, alpha)
            "platinum" ->
                if (type == "start") Color(160, 160, 170, alpha) else Color(230, 230, 240, alpha)
            "titanium" ->
                if (type == "start") Color(140, 140, 150, alpha) else Color(200, 200, 210, alpha)
            "cobalt" ->
                if (type == "start") Color(40, 50, 100, alpha) else Color(80, 100, 180, alpha)
            "nickel" ->
                if (type == "start") Color(100, 110, 100, alpha) else Color(180, 190, 180, alpha)
            "chromium" ->
                if (type == "start") Color(60, 80, 60, alpha) else Color(120, 160, 120, alpha)
            "manganese" ->
                if (type == "start") Color(80, 60, 80, alpha) else Color(160, 120, 160, alpha)
            "vanadium" ->
                if (type == "start") Color(60, 60, 80, alpha) else Color(120, 120, 160, alpha)
            "molybdenum" ->
                if (type == "start") Color(70, 70, 80, alpha) else Color(140, 140, 150, alpha)
            "tungsten" ->
                if (type == "start") Color(60, 60, 60, alpha) else Color(130, 130, 130, alpha)
            "ocean" ->
                if (type == "start") Color(0, 50, 100, alpha) else Color(0, 150, 200, alpha)
            "sunset" ->
                if (type == "start") Color(255, 100, 50, alpha) else Color(255, 180, 100, alpha)
            "forest" ->
                if (type == "start") Color(20, 60, 30, alpha) else Color(60, 140, 60, alpha)
            "midnight" ->
                if (type == "start") Color(10, 10, 40, alpha) else Color(40, 40, 100, alpha)
            "cherry" ->
                if (type == "start") Color(200, 50, 80, alpha) else Color(255, 150, 170, alpha)
            "minty" ->
                if (type == "start") Color(100, 200, 150, alpha) else Color(180, 255, 200, alpha)
            "coralreef" ->
                if (type == "start") Color(255, 100, 120, alpha) else Color(255, 180, 150, alpha)
            "thunder" ->
                if (type == "start") Color(50, 0, 100, alpha) else Color(150, 100, 255, alpha)
            "honey" ->
                if (type == "start") Color(200, 150, 50, alpha) else Color(255, 220, 100, alpha)
            "ice" ->
                if (type == "start") Color(150, 200, 255, alpha) else Color(220, 240, 255, alpha)
            "velvet" ->
                if (type == "start") Color(100, 20, 40, alpha) else Color(180, 60, 80, alpha)
            "moss" ->
                if (type == "start") Color(50, 70, 40, alpha) else Color(100, 130, 70, alpha)
            "plum" ->
                if (type == "start") Color(80, 40, 100, alpha) else Color(150, 80, 180, alpha)
            "sand" ->
                if (type == "start") Color(180, 150, 100, alpha) else Color(230, 200, 150, alpha)
            "storm" ->
                if (type == "start") Color(50, 50, 70, alpha) else Color(100, 100, 130, alpha)
            "peach" ->
                if (type == "start") Color(255, 180, 150, alpha) else Color(255, 220, 200, alpha)
            "denim" ->
                if (type == "start") Color(50, 80, 140, alpha) else Color(100, 140, 200, alpha)
            "olive" ->
                if (type == "start") Color(100, 100, 50, alpha) else Color(150, 150, 80, alpha)
            "wine" ->
                if (type == "start") Color(80, 20, 40, alpha) else Color(150, 50, 70, alpha)
            "sky" ->
                if (type == "start") Color(100, 180, 255, alpha) else Color(180, 220, 255, alpha)
            "amber" ->
                if (type == "start") Color(200, 120, 50, alpha) else Color(255, 180, 80, alpha)
            "fern" ->
                if (type == "start") Color(60, 120, 60, alpha) else Color(120, 180, 100, alpha)
            "iris" ->
                if (type == "start") Color(100, 80, 160, alpha) else Color(180, 140, 220, alpha)
            "crimson" ->
                if (type == "start") Color(150, 20, 40, alpha) else Color(220, 50, 70, alpha)
            "indigo" ->
                if (type == "start") Color(50, 50, 150, alpha) else Color(100, 100, 200, alpha)
            "magenta" ->
                if (type == "start") Color(180, 50, 150, alpha) else Color(255, 100, 200, alpha)
            "ochre" ->
                if (type == "start") Color(180, 120, 50, alpha) else Color(220, 160, 80, alpha)
            "sienna" ->
                if (type == "start") Color(150, 70, 40, alpha) else Color(200, 110, 70, alpha)
            "violet" ->
                if (type == "start") Color(100, 50, 150, alpha) else Color(180, 100, 220, alpha)
            "chartreuse" ->
                if (type == "start") Color(150, 200, 50, alpha) else Color(200, 255, 80, alpha)
            "fuchsia" ->
                if (type == "start") Color(180, 50, 150, alpha) else Color(255, 100, 200, alpha)
            "lime" ->
                if (type == "start") Color(100, 200, 50, alpha) else Color(150, 255, 80, alpha)
            "navy" ->
                if (type == "start") Color(20, 30, 80, alpha) else Color(50, 70, 140, alpha)
            "salmon" ->
                if (type == "start") Color(250, 128, 114, alpha) else Color(255, 180, 170, alpha)
            "tan" ->
                if (type == "start") Color(180, 150, 120, alpha) else Color(220, 190, 160, alpha)
            "khaki" ->
                if (type == "start") Color(180, 170, 100, alpha) else Color(230, 220, 150, alpha)
            "maroon" ->
                if (type == "start") Color(100, 20, 40, alpha) else Color(160, 50, 70, alpha)
            "teal" ->
                if (type == "start") Color(0, 128, 128, alpha) else Color(80, 200, 200, alpha)
            "cyan" ->
                if (type == "start") Color(0, 180, 200, alpha) else Color(100, 230, 255, alpha)
            "bronze" ->
                if (type == "start") Color(140, 100, 50, alpha) else Color(200, 150, 80, alpha)
            "pearl" ->
                if (type == "start") Color(220, 210, 200, alpha) else Color(250, 245, 240, alpha)
            "mahogany" ->
                if (type == "start") Color(120, 40, 40, alpha) else Color(180, 80, 70, alpha)
            "jade2" ->
                if (type == "start") Color(0, 100, 80, alpha) else Color(80, 180, 140, alpha)
            "cobalt2" ->
                if (type == "start") Color(30, 60, 120, alpha) else Color(60, 100, 180, alpha)
            "emerald2" ->
                if (type == "start") Color(0, 120, 80, alpha) else Color(80, 200, 140, alpha)
            "sapphire2" ->
                if (type == "start") Color(20, 50, 120, alpha) else Color(60, 100, 200, alpha)
            "ruby2" ->
                if (type == "start") Color(120, 20, 40, alpha) else Color(200, 50, 80, alpha)
            "topaz2" ->
                if (type == "start") Color(150, 100, 50, alpha) else Color(230, 180, 100, alpha)
            "amethyst3" ->
                if (type == "start") Color(80, 40, 120, alpha) else Color(160, 100, 200, alpha)
            "aquamarine2" ->
                if (type == "start") Color(50, 150, 140, alpha) else Color(120, 220, 200, alpha)
            "garnet2" ->
                if (type == "start") Color(100, 30, 40, alpha) else Color(180, 60, 70, alpha)
            "opal2" ->
                if (type == "start") Color(180, 150, 180, alpha) else Color(230, 200, 230, alpha)
            "tourmaline2" ->
                if (type == "start") Color(30, 120, 100, alpha) else Color(80, 200, 160, alpha)
            "zircon2" ->
                if (type == "start") Color(80, 100, 140, alpha) else Color(140, 170, 210, alpha)
            "peridot2" ->
                if (type == "start") Color(100, 130, 50, alpha) else Color(180, 220, 80, alpha)
            "tanzanite2" ->
                if (type == "start") Color(60, 40, 120, alpha) else Color(120, 80, 200, alpha)
            "spinel2" ->
                if (type == "start") Color(100, 60, 120, alpha) else Color(180, 120, 200, alpha)
            "morganite2" ->
                if (type == "start") Color(200, 140, 160, alpha) else Color(255, 200, 210, alpha)
            "kunzite2" ->
                if (type == "start") Color(180, 130, 170, alpha) else Color(250, 200, 230, alpha)
            "alexandrite2" ->
                if (type == "start") Color(100, 60, 120, alpha) else Color(140, 120, 200, alpha)
            "iolite2" ->
                if (type == "start") Color(60, 60, 120, alpha) else Color(120, 110, 180, alpha)
            "chrysoberyl2" ->
                if (type == "start") Color(130, 130, 50, alpha) else Color(200, 200, 100, alpha)
            "beryl3" ->
                if (type == "start") Color(50, 130, 100, alpha) else Color(100, 200, 160, alpha)
            "corundum2" ->
                if (type == "start") Color(100, 40, 60, alpha) else Color(180, 80, 100, alpha)
            "quartz2" ->
                if (type == "start") Color(200, 200, 210, alpha) else Color(240, 240, 250, alpha)
            "moonstone2" ->
                if (type == "start") Color(190, 190, 210, alpha) else Color(245, 245, 255, alpha)
            "sunstone2" ->
                if (type == "start") Color(180, 100, 50, alpha) else Color(255, 180, 100, alpha)
            "labradorite2" ->
                if (type == "start") Color(50, 60, 80, alpha) else Color(110, 130, 170, alpha)
            "spectrolite2" ->
                if (type == "start") Color(40, 50, 70, alpha) else Color(90, 110, 160, alpha)
            "apatite2" ->
                if (type == "start") Color(30, 100, 130, alpha) else Color(80, 180, 220, alpha)
            "fluorite2" ->
                if (type == "start") Color(100, 80, 140, alpha) else Color(190, 150, 230, alpha)
            "calcite2" ->
                if (type == "start") Color(210, 210, 190, alpha) else Color(255, 255, 240, alpha)
            "sodalite2" ->
                if (type == "start") Color(30, 50, 110, alpha) else Color(70, 100, 180, alpha)
            "lapis2" ->
                if (type == "start") Color(30, 40, 110, alpha) else Color(70, 90, 190, alpha)
            "malachite2" ->
                if (type == "start") Color(20, 90, 50, alpha) else Color(60, 180, 100, alpha)
            "azurite2" ->
                if (type == "start") Color(20, 50, 110, alpha) else Color(60, 110, 210, alpha)
            "rhodochrosite2" ->
                if (type == "start") Color(160, 90, 110, alpha) else Color(255, 150, 180, alpha)
            "rhodonite2" ->
                if (type == "start") Color(110, 60, 70, alpha) else Color(200, 120, 130, alpha)
            "serpentine3" ->
                if (type == "start") Color(50, 90, 50, alpha) else Color(110, 170, 90, alpha)
            "howlite2" ->
                if (type == "start") Color(210, 210, 220, alpha) else Color(250, 250, 255, alpha)
            "onyx2" ->
                if (type == "start") Color(30, 30, 35, alpha) else Color(90, 90, 100, alpha)
            "jasper2" ->
                if (type == "start") Color(130, 70, 50, alpha) else Color(210, 130, 90, alpha)
            "agate2" ->
                if (type == "start") Color(160, 110, 130, alpha) else Color(230, 190, 210, alpha)
            "chert3" ->
                if (type == "start") Color(90, 90, 100, alpha) else Color(160, 160, 170, alpha)
            "flint2" ->
                if (type == "start") Color(60, 60, 70, alpha) else Color(130, 130, 140, alpha)
            "basalt2" ->
                if (type == "start") Color(50, 50, 60, alpha) else Color(110, 110, 120, alpha)
            "granite2" ->
                if (type == "start") Color(160, 130, 110, alpha) else Color(230, 200, 180, alpha)
            "marble2" ->
                if (type == "start") Color(210, 210, 220, alpha) else Color(255, 255, 255, alpha)
            "sandstone2" ->
                if (type == "start") Color(190, 160, 110, alpha) else Color(250, 220, 170, alpha)
            "limestone2" ->
                if (type == "start") Color(190, 190, 180, alpha) else Color(250, 250, 240, alpha)
            "shale2" ->
                if (type == "start") Color(70, 70, 80, alpha) else Color(130, 130, 140, alpha)
            "slate3" ->
                if (type == "start") Color(60, 65, 75, alpha) else Color(110, 120, 130, alpha)
            "quartzite2" ->
                if (type == "start") Color(210, 210, 220, alpha) else Color(255, 255, 255, alpha)
            "gneiss2" ->
                if (type == "start") Color(110, 100, 90, alpha) else Color(190, 180, 170, alpha)
            "schist2" ->
                if (type == "start") Color(90, 90, 80, alpha) else Color(160, 160, 150, alpha)
            "diorite2" ->
                if (type == "start") Color(190, 190, 200, alpha) else Color(250, 250, 255, alpha)
            "andesite2" ->
                if (type == "start") Color(110, 110, 110, alpha) else Color(180, 180, 180, alpha)
            "rhyolite2" ->
                if (type == "start") Color(160, 140, 130, alpha) else Color(230, 210, 200, alpha)
            "pumice2" ->
                if (type == "start") Color(160, 150, 140, alpha) else Color(230, 220, 210, alpha)
            "scoria2" ->
                if (type == "start") Color(70, 50, 50, alpha) else Color(130, 100, 100, alpha)
            "tuff2" ->
                if (type == "start") Color(110, 110, 110, alpha) else Color(170, 170, 170, alpha)
            "dolomite2" ->
                if (type == "start") Color(190, 190, 180, alpha) else Color(250, 250, 240, alpha)
            "halite2" ->
                if (type == "start") Color(210, 230, 250, alpha) else Color(255, 255, 255, alpha)
            "gypsum2" ->
                if (type == "start") Color(210, 210, 200, alpha) else Color(255, 255, 250, alpha)
            "anhydrite2" ->
                if (type == "start") Color(190, 190, 210, alpha) else Color(240, 240, 255, alpha)
            "barite2" ->
                if (type == "start") Color(210, 210, 230, alpha) else Color(255, 255, 255, alpha)
            "celestine2" ->
                if (type == "start") Color(160, 190, 230, alpha) else Color(210, 230, 255, alpha)
            "anglesite2" ->
                if (type == "start") Color(210, 210, 190, alpha) else Color(255, 255, 240, alpha)
            "galena2" ->
                if (type == "start") Color(70, 70, 80, alpha) else Color(130, 130, 140, alpha)
            "sphalerite2" ->
                if (type == "start") Color(90, 70, 40, alpha) else Color(190, 150, 90, alpha)
            "cinnabar2" ->
                if (type == "start") Color(110, 30, 30, alpha) else Color(230, 60, 60, alpha)
            "stibnite2" ->
                if (type == "start") Color(60, 60, 70, alpha) else Color(130, 130, 140, alpha)
            "orpiment2" ->
                if (type == "start") Color(190, 150, 10, alpha) else Color(255, 210, 60, alpha)
            "realgar2" ->
                if (type == "start") Color(130, 50, 10, alpha) else Color(230, 90, 30, alpha)
            "copper2" ->
                if (type == "start") Color(130, 70, 40, alpha) else Color(230, 140, 80, alpha)
            "silver2" ->
                if (type == "start") Color(160, 160, 170, alpha) else Color(230, 230, 240, alpha)
            "gold2" ->
                if (type == "start") Color(130, 100, 10, alpha) else Color(255, 210, 60, alpha)
            "platinum2" ->
                if (type == "start") Color(170, 170, 180, alpha) else Color(240, 240, 250, alpha)
            "titanium2" ->
                if (type == "start") Color(150, 150, 160, alpha) else Color(210, 210, 220, alpha)
            "cobalt3" ->
                if (type == "start") Color(50, 60, 110, alpha) else Color(90, 110, 190, alpha)
            "nickel2" ->
                if (type == "start") Color(110, 120, 110, alpha) else Color(190, 200, 190, alpha)
            "chromium2" ->
                if (type == "start") Color(70, 90, 70, alpha) else Color(130, 170, 130, alpha)
            "manganese2" ->
                if (type == "start") Color(90, 70, 90, alpha) else Color(170, 130, 170, alpha)
            "vanadium2" ->
                if (type == "start") Color(70, 70, 90, alpha) else Color(130, 130, 170, alpha)
            "molybdenum2" ->
                if (type == "start") Color(80, 80, 90, alpha) else Color(150, 150, 160, alpha)
            "tungsten2" ->
                if (type == "start") Color(70, 70, 70, alpha) else Color(140, 140, 140, alpha)
            "astolfo" ->
                if (type == "start")
                    ColorUtils.skyRainbow(0, 0.6f, 1f, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }
                else
                    ColorUtils.skyRainbow(90, 0.6f, 1f, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }

            "rainbow" ->
                if (type == "start")
                    ColorUtils.skyRainbow(0, 1f, 1f, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }
                else
                    ColorUtils.skyRainbow(90, 1f, 1f, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }

            else -> Color(-1)
        }
    }

    fun getColor(index: Int = 0): Color {
        val mode = ClientColorMode.lowercase()
        val fadeVal = ThemeFadeSpeed / 5.0 * if (updown) 1 else -1
        if (mode.startsWith("#")) {
            return parseHexColor(mode)
        }
        val colorMap = mapOf(
            "zywl"      to Pair(Color(206, 58, 98),   Color(215, 171, 168)),
            "water"     to Pair(Color(35, 69, 148),   Color(108, 170, 207)),
            "magic"     to Pair(Color(255, 180, 255), Color(181, 139, 194)),
            "tree"      to Pair(Color(18, 155, 38),   Color(76, 255, 102)),
            "darknight" to Pair(Color(93, 95, 95),    Color(203, 200, 204)),
            "sun"       to Pair(Color(255, 143, 0),   Color(252, 205, 44)),
            "flower"    to Pair(Color(184, 85, 199),  Color(182, 140, 195)),
            "loyoi"     to Pair(Color(255, 131, 0),   Color(255, 131, 124)),
            "fdp"       to Pair(Color(255, 100, 255), Color(100, 255, 255)),
            "may"       to Pair(Color(255, 80, 255),  Color(255, 255, 255)),
            "mint"      to Pair(Color(85, 255, 140),  Color(85, 255, 255)),
            "cero"      to Pair(Color(170, 0, 170),   Color(170, 255, 170)),
            "azure"     to Pair(Color(0, 90, 255),    Color(0, 180, 255)),
            "pumpkin"   to Pair(Color(255, 216, 169), Color(241, 166, 98)),
            "polarized" to Pair(Color(0, 32, 64),     Color(173, 239, 209)),
            "sundae"    to Pair(Color(28, 28, 27),    Color(206, 74, 126)),
            "terminal"  to Pair(Color(25, 30, 25),    Color(15, 155, 15)),
            "coral"     to Pair(Color(52, 133, 151),  Color(244, 168, 150)),
            "fire"      to Pair(Color(255, 45, 30),   Color(255, 123, 15)),
            "aqua"      to Pair(Color(80, 255, 255),  Color(80, 190, 255)),
            "peony"     to Pair(Color(255, 120, 255), Color(255, 190, 255)),
            "vergren"   to Pair(Color(170, 255, 169), Color(17, 255, 189)),
            "eveningsunshine" to Pair(Color(185, 43, 39), Color(21, 101, 192)),
            "lightorange"     to Pair(Color(255, 183, 94), Color(237, 143, 3)),
            "reef"             to Pair(Color(0, 210, 255), Color(58, 123, 213)),
            "amin"             to Pair(Color(142, 45, 226), Color(74, 0, 224)),
            "magics"           to Pair(Color(89, 193, 115), Color(93, 38, 193)),
            "mangopulp"        to Pair(Color(240, 152, 25), Color(237, 222, 93)),
            "moonpurple"       to Pair(Color(78, 84, 200), Color(143, 148, 251)),
            "aqualicious"      to Pair(Color(80, 201, 195), Color(150, 222, 218)),
            "stripe"           to Pair(Color(31, 162, 255), Color(166, 255, 203)),
            "shifter"          to Pair(Color(188, 78, 156), Color(248, 7, 89)),
            "quepal"           to Pair(Color(17, 153, 142), Color(56, 239, 125)),
            "orca"             to Pair(Color(68, 160, 141), Color(9, 54, 55)),
            "sublimevivid"     to Pair(Color(252, 70, 107), Color(63, 94, 251)),
            "moonasteroid"     to Pair(Color(15, 32, 39), Color(44, 83, 100)),
            "summerdog"        to Pair(Color(168, 255, 120), Color(120, 255, 214)),
            "pinkflavour"      to Pair(Color(128, 0, 128), Color(255, 192, 203)),
            "sincityred"       to Pair(Color(237, 33, 58), Color(147, 41, 30)),
            "timber"           to Pair(Color(252, 0, 255), Color(0, 219, 222)),
            "pinotnoir"        to Pair(Color(75, 108, 183), Color(24, 40, 72)),
            "dirtyfog"         to Pair(Color(185, 147, 214), Color(140, 166, 219)),
            "piglet"           to Pair(Color(238, 156, 167), Color(255, 221, 225)),
            "littleleaf"       to Pair(Color(118, 184, 82), Color(141, 194, 111)),
            "nelson"           to Pair(Color(242, 112, 156), Color(255, 148, 114)),
            "turquoiseflow"    to Pair(Color(19, 106, 138), Color(38, 120, 113)),
            "purplin"          to Pair(Color(106, 48, 147), Color(160, 68, 255)),
            "martini"          to Pair(Color(253, 252, 71), Color(36, 254, 65)),
            "soundcloud"       to Pair(Color(254, 140, 0), Color(248, 54, 0)),
            "inbox"            to Pair(Color(69, 127, 202), Color(86, 145, 200)),
            "amethyst"         to Pair(Color(157, 80, 187), Color(110, 72, 170)),
            "blush"            to Pair(Color(178, 69, 146), Color(241, 95, 121)),
            "mocharose"        to Pair(Color(245, 194, 231), Color(243, 139, 168)),
            "neoncrimson"      to Pair(Color(10, 0, 15), Color(255, 20, 80)),
            "acidgreen"        to Pair(Color(15, 15, 15), Color(57, 255, 20)),
            "vaporwave"        to Pair(Color(20, 0, 40), Color(255, 100, 200)),
            "noir"             to Pair(Color(12, 12, 12), Color(230, 230, 230)),
            "obsidian"         to Pair(Color(5, 5, 5), Color(40, 40, 45)),
            "champagne"        to Pair(Color(60, 20, 25), Color(245, 215, 160)),
            "rosegold"         to Pair(Color(45, 20, 35), Color(220, 170, 160)),
            "arctic"           to Pair(Color(200, 220, 235), Color(240, 248, 255)),
            "frost"            to Pair(Color(180, 210, 230), Color(230, 245, 255)),
            "glacier"          to Pair(Color(30, 60, 90), Color(140, 200, 230)),
            "slate"            to Pair(Color(40, 42, 54), Color(98, 114, 164)),
            "abyss"            to Pair(Color(0, 8, 20), Color(15, 80, 130)),
            "biolum"           to Pair(Color(0, 15, 20), Color(0, 255, 180)),
            "evergreen"        to Pair(Color(10, 30, 15), Color(80, 200, 100)),
            "dusk"             to Pair(Color(40, 15, 50), Color(220, 100, 50)),
            "aurora"           to Pair(Color(10, 15, 40), Color(80, 255, 180)),
            "retrowave"        to Pair(Color(30, 0, 50), Color(255, 0, 110)),
            "y2k"              to Pair(Color(180, 120, 255), Color(255, 200, 230)),
            "dustyrose"        to Pair(Color(180, 140, 150), Color(220, 190, 195)),
            "sage"             to Pair(Color(140, 160, 140), Color(190, 210, 180)),
            "cloudburst"       to Pair(Color(150, 160, 180), Color(200, 210, 220)),
            "monolith"         to Pair(Color(0, 0, 0), Color(255, 255, 255)),
            "bloodline"        to Pair(Color(40, 0, 0), Color(200, 20, 20)),
            "lavender"         to Pair(Color(200, 180, 220), Color(230, 220, 240)),
            "butter"           to Pair(Color(240, 220, 170), Color(255, 245, 200)),
            "gothic"           to Pair(Color(8, 8, 12), Color(60, 10, 20)),
            "phantom"          to Pair(Color(20, 25, 40), Color(100, 120, 180)),
            "quicksilver"      to Pair(Color(180, 190, 200), Color(220, 230, 240)),
            "mercury"          to Pair(Color(40, 30, 60), Color(190, 200, 210)),
            "tropical"         to Pair(Color(0, 80, 100), Color(255, 220, 50)),
            "mango"            to Pair(Color(100, 30, 60), Color(255, 150, 50)),
            "rust"             to Pair(Color(60, 30, 20), Color(180, 80, 30)),
            "concrete"         to Pair(Color(90, 90, 90), Color(130, 130, 130)),
            "nebula"           to Pair(Color(5, 0, 15), Color(120, 50, 180)),
            "supernova"        to Pair(Color(10, 10, 30), Color(100, 150, 255)),
            "eclipse"          to Pair(Color(15, 5, 25), Color(80, 0, 120)),
            "iceberg"          to Pair(Color(150, 200, 220), Color(220, 240, 250)),
            "scarlet"          to Pair(Color(80, 10, 20), Color(230, 50, 50)),
            "cyberpink"        to Pair(Color(20, 0, 30), Color(255, 50, 150)),
            "matrix"           to Pair(Color(0, 10, 0), Color(0, 180, 0)),
            "solarglare"       to Pair(Color(40, 30, 0), Color(255, 200, 50)),
            "emerald"          to Pair(Color(0, 80, 40), Color(80, 200, 120)),
            "sapphire"         to Pair(Color(0, 30, 80), Color(50, 100, 200)),
            "ruby"             to Pair(Color(80, 10, 20), Color(220, 30, 60)),
            "topaz"            to Pair(Color(100, 60, 0), Color(255, 180, 50)),
            "amethyst2"        to Pair(Color(60, 20, 100), Color(180, 100, 220)),
            "jade"             to Pair(Color(0, 80, 50), Color(100, 180, 120)),
            "opal"             to Pair(Color(150, 100, 150), Color(200, 180, 220)),
            "garnet"           to Pair(Color(60, 10, 20), Color(180, 40, 60)),
            "turquoise"        to Pair(Color(0, 80, 100), Color(50, 200, 220)),
            "citrine"          to Pair(Color(120, 80, 0), Color(255, 200, 50)),
            "peridot"          to Pair(Color(80, 100, 0), Color(200, 220, 50)),
            "aquamarine"       to Pair(Color(0, 100, 120), Color(100, 220, 200)),
            "tanzanite"        to Pair(Color(40, 20, 100), Color(120, 80, 200)),
            "morganite"        to Pair(Color(150, 100, 120), Color(255, 180, 200)),
            "kunzite"          to Pair(Color(150, 100, 150), Color(255, 180, 220)),
            "spinel"           to Pair(Color(80, 40, 100), Color(200, 120, 180)),
            "zircon"           to Pair(Color(60, 80, 120), Color(150, 180, 220)),
            "tourmaline"       to Pair(Color(0, 100, 80), Color(100, 200, 150)),
            "alexandrite"      to Pair(Color(80, 40, 100), Color(100, 150, 200)),
            "iolite"           to Pair(Color(40, 40, 100), Color(120, 100, 180)),
            "chrysoberyl"      to Pair(Color(100, 100, 0), Color(220, 200, 80)),
            "beryl"            to Pair(Color(0, 100, 80), Color(80, 200, 180)),
            "corundum"         to Pair(Color(80, 20, 40), Color(200, 80, 100)),
            "beryl2"           to Pair(Color(60, 120, 80), Color(120, 200, 140)),
            "quartz"           to Pair(Color(180, 180, 200), Color(220, 220, 240)),
            "moonstone"        to Pair(Color(180, 180, 200), Color(240, 240, 255)),
            "sunstone"         to Pair(Color(150, 80, 30), Color(255, 180, 100)),
            "labradorite"      to Pair(Color(40, 50, 70), Color(120, 140, 180)),
            "spectrolite"      to Pair(Color(30, 40, 60), Color(100, 120, 180)),
            "apatite"          to Pair(Color(0, 80, 120), Color(80, 180, 220)),
            "fluorite"         to Pair(Color(80, 60, 120), Color(180, 140, 220)),
            "calcite"          to Pair(Color(200, 200, 180), Color(255, 255, 230)),
            "sodalite"         to Pair(Color(20, 40, 100), Color(60, 100, 180)),
            "lapis"            to Pair(Color(20, 30, 100), Color(60, 80, 180)),
            "malachite"        to Pair(Color(0, 80, 40), Color(50, 180, 100)),
            "azurite"          to Pair(Color(0, 40, 100), Color(50, 100, 200)),
            "rhodochrosite"    to Pair(Color(150, 80, 100), Color(255, 150, 180)),
            "rhodonite"        to Pair(Color(100, 50, 60), Color(200, 120, 130)),
            "serpentine"       to Pair(Color(40, 80, 40), Color(100, 160, 80)),
            "serpentine2"      to Pair(Color(60, 100, 50), Color(120, 180, 100)),
            "howlite"          to Pair(Color(200, 200, 210), Color(240, 240, 250)),
            "obsidian2"        to Pair(Color(20, 15, 25), Color(60, 50, 70)),
            "onyx"             to Pair(Color(20, 20, 25), Color(80, 80, 90)),
            "jasper"           to Pair(Color(120, 60, 40), Color(200, 120, 80)),
            "agate"            to Pair(Color(150, 100, 120), Color(220, 180, 200)),
            "chert"            to Pair(Color(80, 80, 90), Color(150, 150, 160)),
            "flint"            to Pair(Color(50, 50, 60), Color(120, 120, 130)),
            "chert2"           to Pair(Color(60, 70, 80), Color(130, 140, 150)),
            "basalt"           to Pair(Color(40, 40, 50), Color(100, 100, 110)),
            "granite"          to Pair(Color(150, 120, 100), Color(220, 190, 170)),
            "marble"           to Pair(Color(200, 200, 210), Color(250, 250, 255)),
            "sandstone"        to Pair(Color(180, 150, 100), Color(240, 210, 160)),
            "limestone"        to Pair(Color(180, 180, 170), Color(240, 240, 230)),
            "shale"            to Pair(Color(60, 60, 70), Color(120, 120, 130)),
            "slate2"           to Pair(Color(50, 55, 65), Color(100, 110, 120)),
            "quartzite"        to Pair(Color(200, 200, 210), Color(255, 255, 255)),
            "gneiss"           to Pair(Color(100, 90, 80), Color(180, 170, 160)),
            "schist"           to Pair(Color(80, 80, 70), Color(150, 150, 140)),
            "diorite"          to Pair(Color(180, 180, 190), Color(240, 240, 250)),
            "andesite"         to Pair(Color(100, 100, 100), Color(170, 170, 170)),
            "rhyolite"         to Pair(Color(150, 130, 120), Color(220, 200, 190)),
            "pumice"           to Pair(Color(150, 140, 130), Color(220, 210, 200)),
            "scoria"           to Pair(Color(60, 40, 40), Color(120, 90, 90)),
            "tuff"             to Pair(Color(100, 100, 100), Color(160, 160, 160)),
            "dolomite"         to Pair(Color(180, 180, 170), Color(240, 240, 230)),
            "halite"           to Pair(Color(200, 220, 240), Color(255, 255, 255)),
            "gypsum"           to Pair(Color(200, 200, 190), Color(255, 255, 245)),
            "anhydrite"        to Pair(Color(180, 180, 200), Color(230, 230, 250)),
            "barite"           to Pair(Color(200, 200, 220), Color(255, 255, 255)),
            "celestine"        to Pair(Color(150, 180, 220), Color(200, 220, 255)),
            "anglesite"        to Pair(Color(200, 200, 180), Color(255, 255, 230)),
            "galena"           to Pair(Color(60, 60, 70), Color(120, 120, 130)),
            "sphalerite"       to Pair(Color(80, 60, 30), Color(180, 140, 80)),
            "cinnabar"         to Pair(Color(100, 20, 20), Color(220, 50, 50)),
            "stibnite"         to Pair(Color(50, 50, 60), Color(120, 120, 130)),
            "orpiment"         to Pair(Color(180, 140, 0), Color(255, 200, 50)),
            "realgar"          to Pair(Color(120, 40, 0), Color(220, 80, 20)),
            "copper"           to Pair(Color(120, 60, 30), Color(220, 130, 70)),
            "silver"           to Pair(Color(150, 150, 160), Color(220, 220, 230)),
            "gold"             to Pair(Color(120, 90, 0), Color(255, 200, 50)),
            "platinum"         to Pair(Color(160, 160, 170), Color(230, 230, 240)),
            "titanium"         to Pair(Color(140, 140, 150), Color(200, 200, 210)),
            "cobalt"           to Pair(Color(40, 50, 100), Color(80, 100, 180)),
            "nickel"           to Pair(Color(100, 110, 100), Color(180, 190, 180)),
            "chromium"         to Pair(Color(60, 80, 60), Color(120, 160, 120)),
            "manganese"        to Pair(Color(80, 60, 80), Color(160, 120, 160)),
            "vanadium"         to Pair(Color(60, 60, 80), Color(120, 120, 160)),
            "molybdenum"       to Pair(Color(70, 70, 80), Color(140, 140, 150)),
            "tungsten"         to Pair(Color(60, 60, 60), Color(130, 130, 130)),
            "ocean"            to Pair(Color(0, 50, 100), Color(0, 150, 200)),
            "sunset"           to Pair(Color(255, 100, 50), Color(255, 180, 100)),
            "forest"           to Pair(Color(20, 60, 30), Color(60, 140, 60)),
            "midnight"         to Pair(Color(10, 10, 40), Color(40, 40, 100)),
            "cherry"           to Pair(Color(200, 50, 80), Color(255, 150, 170)),
            "minty"            to Pair(Color(100, 200, 150), Color(180, 255, 200)),
            "coralreef"        to Pair(Color(255, 100, 120), Color(255, 180, 150)),
            "thunder"          to Pair(Color(50, 0, 100), Color(150, 100, 255)),
            "honey"            to Pair(Color(200, 150, 50), Color(255, 220, 100)),
            "ice"              to Pair(Color(150, 200, 255), Color(220, 240, 255)),
            "velvet"           to Pair(Color(100, 20, 40), Color(180, 60, 80)),
            "moss"             to Pair(Color(50, 70, 40), Color(100, 130, 70)),
            "plum"             to Pair(Color(80, 40, 100), Color(150, 80, 180)),
            "sand"             to Pair(Color(180, 150, 100), Color(230, 200, 150)),
            "storm"            to Pair(Color(50, 50, 70), Color(100, 100, 130)),
            "peach"            to Pair(Color(255, 180, 150), Color(255, 220, 200)),
            "denim"            to Pair(Color(50, 80, 140), Color(100, 140, 200)),
            "olive"            to Pair(Color(100, 100, 50), Color(150, 150, 80)),
            "wine"             to Pair(Color(80, 20, 40), Color(150, 50, 70)),
            "sky"              to Pair(Color(100, 180, 255), Color(180, 220, 255)),
            "amber"            to Pair(Color(200, 120, 50), Color(255, 180, 80)),
            "fern"             to Pair(Color(60, 120, 60), Color(120, 180, 100)),
            "iris"             to Pair(Color(100, 80, 160), Color(180, 140, 220)),
            "crimson"          to Pair(Color(150, 20, 40), Color(220, 50, 70)),
            "indigo"           to Pair(Color(50, 50, 150), Color(100, 100, 200)),
            "magenta"          to Pair(Color(180, 50, 150), Color(255, 100, 200)),
            "ochre"            to Pair(Color(180, 120, 50), Color(220, 160, 80)),
            "sienna"           to Pair(Color(150, 70, 40), Color(200, 110, 70)),
            "violet"           to Pair(Color(100, 50, 150), Color(180, 100, 220)),
            "chartreuse"       to Pair(Color(150, 200, 50), Color(200, 255, 80)),
            "fuchsia"          to Pair(Color(180, 50, 150), Color(255, 100, 200)),
            "lime"             to Pair(Color(100, 200, 50), Color(150, 255, 80)),
            "navy"             to Pair(Color(20, 30, 80), Color(50, 70, 140)),
            "salmon"           to Pair(Color(250, 128, 114), Color(255, 180, 170)),
            "tan"              to Pair(Color(180, 150, 120), Color(220, 190, 160)),
            "khaki"            to Pair(Color(180, 170, 100), Color(230, 220, 150)),
            "maroon"           to Pair(Color(100, 20, 40), Color(160, 50, 70)),
            "teal"             to Pair(Color(0, 128, 128), Color(80, 200, 200)),
            "cyan"             to Pair(Color(0, 180, 200), Color(100, 230, 255)),
            "bronze"           to Pair(Color(140, 100, 50), Color(200, 150, 80)),
            "pearl"            to Pair(Color(220, 210, 200), Color(250, 245, 240)),
            "mahogany"         to Pair(Color(120, 40, 40), Color(180, 80, 70)),
            "jade2"            to Pair(Color(0, 100, 80), Color(80, 180, 140)),
            "cobalt2"          to Pair(Color(30, 60, 120), Color(60, 100, 180)),
            "emerald2"         to Pair(Color(0, 120, 80), Color(80, 200, 140)),
            "sapphire2"        to Pair(Color(20, 50, 120), Color(60, 100, 200)),
            "ruby2"            to Pair(Color(120, 20, 40), Color(200, 50, 80)),
            "topaz2"           to Pair(Color(150, 100, 50), Color(230, 180, 100)),
            "amethyst3"        to Pair(Color(80, 40, 120), Color(160, 100, 200)),
            "aquamarine2"      to Pair(Color(50, 150, 140), Color(120, 220, 200)),
            "garnet2"          to Pair(Color(100, 30, 40), Color(180, 60, 70)),
            "opal2"            to Pair(Color(180, 150, 180), Color(230, 200, 230)),
            "tourmaline2"      to Pair(Color(30, 120, 100), Color(80, 200, 160)),
            "zircon2"          to Pair(Color(80, 100, 140), Color(140, 170, 210)),
            "peridot2"         to Pair(Color(100, 130, 50), Color(180, 220, 80)),
            "tanzanite2"       to Pair(Color(60, 40, 120), Color(120, 80, 200)),
            "spinel2"          to Pair(Color(100, 60, 120), Color(180, 120, 200)),
            "morganite2"       to Pair(Color(200, 140, 160), Color(255, 200, 210)),
            "kunzite2"         to Pair(Color(180, 130, 170), Color(250, 200, 230)),
            "alexandrite2"     to Pair(Color(100, 60, 120), Color(140, 120, 200)),
            "iolite2"          to Pair(Color(60, 60, 120), Color(120, 110, 180)),
            "chrysoberyl2"     to Pair(Color(130, 130, 50), Color(200, 200, 100)),
            "beryl3"           to Pair(Color(50, 130, 100), Color(100, 200, 160)),
            "corundum2"        to Pair(Color(100, 40, 60), Color(180, 80, 100)),
            "quartz2"          to Pair(Color(200, 200, 210), Color(240, 240, 250)),
            "moonstone2"       to Pair(Color(190, 190, 210), Color(245, 245, 255)),
            "sunstone2"        to Pair(Color(180, 100, 50), Color(255, 180, 100)),
            "labradorite2"     to Pair(Color(50, 60, 80), Color(110, 130, 170)),
            "spectrolite2"     to Pair(Color(40, 50, 70), Color(90, 110, 160)),
            "apatite2"         to Pair(Color(30, 100, 130), Color(80, 180, 220)),
            "fluorite2"        to Pair(Color(100, 80, 140), Color(190, 150, 230)),
            "calcite2"         to Pair(Color(210, 210, 190), Color(255, 255, 240)),
            "sodalite2"        to Pair(Color(30, 50, 110), Color(70, 100, 180)),
            "lapis2"           to Pair(Color(30, 40, 110), Color(70, 90, 190)),
            "malachite2"       to Pair(Color(20, 90, 50), Color(60, 180, 100)),
            "azurite2"         to Pair(Color(20, 50, 110), Color(60, 110, 210)),
            "rhodochrosite2"   to Pair(Color(160, 90, 110), Color(255, 150, 180)),
            "rhodonite2"       to Pair(Color(110, 60, 70), Color(200, 120, 130)),
            "serpentine3"      to Pair(Color(50, 90, 50), Color(110, 170, 90)),
            "howlite2"         to Pair(Color(210, 210, 220), Color(250, 250, 255)),
            "onyx2"            to Pair(Color(30, 30, 35), Color(90, 90, 100)),
            "jasper2"          to Pair(Color(130, 70, 50), Color(210, 130, 90)),
            "agate2"           to Pair(Color(160, 110, 130), Color(230, 190, 210)),
            "chert3"           to Pair(Color(90, 90, 100), Color(160, 160, 170)),
            "flint2"           to Pair(Color(60, 60, 70), Color(130, 130, 140)),
            "basalt2"          to Pair(Color(50, 50, 60), Color(110, 110, 120)),
            "granite2"         to Pair(Color(160, 130, 110), Color(230, 200, 180)),
            "marble2"          to Pair(Color(210, 210, 220), Color(255, 255, 255)),
            "sandstone2"       to Pair(Color(190, 160, 110), Color(250, 220, 170)),
            "limestone2"       to Pair(Color(190, 190, 180), Color(250, 250, 240)),
            "shale2"           to Pair(Color(70, 70, 80), Color(130, 130, 140)),
            "slate3"           to Pair(Color(60, 65, 75), Color(110, 120, 130)),
            "quartzite2"       to Pair(Color(210, 210, 220), Color(255, 255, 255)),
            "gneiss2"          to Pair(Color(110, 100, 90), Color(190, 180, 170)),
            "schist2"          to Pair(Color(90, 90, 80), Color(160, 160, 150)),
            "diorite2"         to Pair(Color(190, 190, 200), Color(250, 250, 255)),
            "andesite2"        to Pair(Color(110, 110, 110), Color(180, 180, 180)),
            "rhyolite2"        to Pair(Color(160, 140, 130), Color(230, 210, 200)),
            "pumice2"          to Pair(Color(160, 150, 140), Color(230, 220, 210)),
            "scoria2"          to Pair(Color(70, 50, 50), Color(130, 100, 100)),
            "tuff2"            to Pair(Color(110, 110, 110), Color(170, 170, 170)),
            "dolomite2"        to Pair(Color(190, 190, 180), Color(250, 250, 240)),
            "halite2"          to Pair(Color(210, 230, 250), Color(255, 255, 255)),
            "gypsum2"          to Pair(Color(210, 210, 200), Color(255, 255, 250)),
            "anhydrite2"       to Pair(Color(190, 190, 210), Color(240, 240, 255)),
            "barite2"          to Pair(Color(210, 210, 230), Color(255, 255, 255)),
            "celestine2"       to Pair(Color(160, 190, 230), Color(210, 230, 255)),
            "anglesite2"       to Pair(Color(210, 210, 190), Color(255, 255, 240)),
            "galena2"          to Pair(Color(70, 70, 80), Color(130, 130, 140)),
            "sphalerite2"      to Pair(Color(90, 70, 40), Color(190, 150, 90)),
            "cinnabar2"        to Pair(Color(110, 30, 30), Color(230, 60, 60)),
            "stibnite2"        to Pair(Color(60, 60, 70), Color(130, 130, 140)),
            "orpiment2"        to Pair(Color(190, 150, 10), Color(255, 210, 60)),
            "realgar2"         to Pair(Color(130, 50, 10), Color(230, 90, 30)),
            "copper2"          to Pair(Color(130, 70, 40), Color(230, 140, 80)),
            "silver2"          to Pair(Color(160, 160, 170), Color(230, 230, 240)),
            "gold2"            to Pair(Color(130, 100, 10), Color(255, 210, 60)),
            "platinum2"        to Pair(Color(170, 170, 180), Color(240, 240, 250)),
            "titanium2"        to Pair(Color(150, 150, 160), Color(210, 210, 220)),
            "cobalt3"          to Pair(Color(50, 60, 110), Color(90, 110, 190)),
            "nickel2"          to Pair(Color(110, 120, 110), Color(190, 200, 190)),
            "chromium2"        to Pair(Color(70, 90, 70), Color(130, 170, 130)),
            "manganese2"       to Pair(Color(90, 70, 90), Color(170, 130, 170)),
            "vanadium2"        to Pair(Color(70, 70, 90), Color(130, 130, 170)),
            "molybdenum2"      to Pair(Color(80, 80, 90), Color(150, 150, 160)),
            "tungsten2"        to Pair(Color(70, 70, 70), Color(140, 140, 140)),
            "astolfo"   to Pair(
                ColorUtils.skyRainbow(0, 0.6F, 1F, 20000F / ThemeFadeSpeed),
                ColorUtils.skyRainbow(90, 0.6F, 1F, 20000F / ThemeFadeSpeed)
            ),
            "rainbow"   to Pair(
                ColorUtils.skyRainbow(0, 1F, 1F, 20000F / ThemeFadeSpeed),
                ColorUtils.skyRainbow(90, 1F, 1F, 20000F / ThemeFadeSpeed)
            )
        )

        val colorPair = colorMap[mode] ?: return Color(-1)
        return mixColors(colorPair.first, colorPair.second, fadeVal, index)
    }

    fun getColorWithAlpha(index: Int, alpha: Int): Color {
        val mode = ClientColorMode.lowercase()
        val fadeSpeed = (ThemeFadeSpeed / 5.0) * if (updown) 1 else -1

        if (mode.startsWith("#")) {
            return parseHexColor(mode).let { Color(it.red, it.green, it.blue, alpha) }
        }
        return when (mode) {
            "zywl"      -> mixColors(Color(206, 58, 98),   Color(215, 171, 168), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "water"     -> mixColors(Color(35, 69, 148),   Color(108, 170, 207), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "magic"     -> mixColors(Color(255, 180, 255), Color(181, 139, 194), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tree"      -> mixColors(Color(18, 155, 38),   Color(76, 255, 102), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "darknight" -> mixColors(Color(93, 95, 95),    Color(203, 200, 204), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sun"       -> mixColors(Color(255, 143, 0),   Color(252, 205, 44), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "flower"    -> mixColors(Color(184, 85, 199),  Color(182, 140, 195), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "loyoi"     -> mixColors(Color(255, 131, 0),   Color(255, 131, 124), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fdp"       -> mixColors(Color(255, 100, 255), Color(100, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "may"       -> mixColors(Color(255, 80, 255),  Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mint"      -> mixColors(Color(85, 255, 180),  Color(85, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cero"      -> mixColors(Color(170, 0, 170),   Color(170, 255, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "azure"     -> mixColors(Color(0, 90, 255),    Color(0, 180, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pumpkin"   -> mixColors(Color(255, 216, 169), Color(241, 166, 98), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "polarized" -> mixColors(Color(0, 32, 64),     Color(173, 239, 209), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sundae"    -> mixColors(Color(28, 28, 27),    Color(206, 74, 126), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "terminal"  -> mixColors(Color(25, 30, 25),    Color(15, 155, 15), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "coral"     -> mixColors(Color(52, 133, 151),  Color(244, 168, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fire"      -> mixColors(Color(255, 45, 30),   Color(255, 123, 15), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "aqua"      -> mixColors(Color(80, 255, 255),  Color(80, 190, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "peony"     -> mixColors(Color(255, 120, 255), Color(255, 190, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "vergren"   -> mixColors(Color(170, 255, 169), Color(17, 255, 189), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "eveningsunshine" -> mixColors(Color(185, 43, 39), Color(21, 101, 192), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "lightorange" -> mixColors(Color(255, 183, 94), Color(237, 143, 3), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "reef" -> mixColors(Color(0, 210, 255), Color(58, 123, 213), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "amin" -> mixColors(Color(142, 45, 226), Color(74, 0, 224), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "magics" -> mixColors(Color(89, 193, 115), Color(93, 38, 193), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mangopulp" -> mixColors(Color(240, 152, 25), Color(237, 222, 93), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "moonpurple" -> mixColors(Color(78, 84, 200), Color(143, 148, 251), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "aqualicious" -> mixColors(Color(80, 201, 195), Color(150, 222, 218), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "stripe" -> mixColors(Color(31, 162, 255), Color(166, 255, 203), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "shifter" -> mixColors(Color(188, 78, 156), Color(248, 7, 89), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quepal" -> mixColors(Color(17, 153, 142), Color(56, 239, 125), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "orca" -> mixColors(Color(68, 160, 141), Color(9, 54, 55), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sublimevivid" -> mixColors(Color(252, 70, 107), Color(63, 94, 251), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "moonasteroid" -> mixColors(Color(15, 32, 39), Color(44, 83, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "summerdog" -> mixColors(Color(168, 255, 120), Color(120, 255, 214), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pinkflavour" -> mixColors(Color(128, 0, 128), Color(255, 192, 203), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sincityred" -> mixColors(Color(237, 33, 58), Color(147, 41, 30), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "timber" -> mixColors(Color(252, 0, 255), Color(0, 219, 222), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pinotnoir" -> mixColors(Color(75, 108, 183), Color(24, 40, 72), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "dirtyfog" -> mixColors(Color(185, 147, 214), Color(140, 166, 219), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "piglet" -> mixColors(Color(238, 156, 167), Color(255, 221, 225), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "littleleaf" -> mixColors(Color(118, 184, 82), Color(141, 194, 111), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "nelson" -> mixColors(Color(242, 112, 156), Color(255, 148, 114), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "turquoiseflow" -> mixColors(Color(19, 106, 138), Color(38, 120, 113), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "purplin" -> mixColors(Color(106, 48, 147), Color(160, 68, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "martini" -> mixColors(Color(253, 252, 71), Color(36, 254, 65), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "soundcloud" -> mixColors(Color(254, 140, 0), Color(248, 54, 0), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "inbox" -> mixColors(Color(69, 127, 202), Color(86, 145, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "amethyst" -> mixColors(Color(157, 80, 187), Color(110, 72, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "blush" -> mixColors(Color(178, 69, 146), Color(241, 95, 121), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mocharose" -> mixColors(Color(245, 194, 231), Color(243, 139, 168), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "neoncrimson" -> mixColors(Color(10, 0, 15), Color(255, 20, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "acidgreen" -> mixColors(Color(15, 15, 15), Color(57, 255, 20), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "vaporwave" -> mixColors(Color(20, 0, 40), Color(255, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "noir" -> mixColors(Color(12, 12, 12), Color(230, 230, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "obsidian" -> mixColors(Color(5, 5, 5), Color(40, 40, 45), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "champagne" -> mixColors(Color(60, 20, 25), Color(245, 215, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rosegold" -> mixColors(Color(45, 20, 35), Color(220, 170, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "arctic" -> mixColors(Color(200, 220, 235), Color(240, 248, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "frost" -> mixColors(Color(180, 210, 230), Color(230, 245, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "glacier" -> mixColors(Color(30, 60, 90), Color(140, 200, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "slate" -> mixColors(Color(40, 42, 54), Color(98, 114, 164), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "abyss" -> mixColors(Color(0, 8, 20), Color(15, 80, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "biolum" -> mixColors(Color(0, 15, 20), Color(0, 255, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "evergreen" -> mixColors(Color(10, 30, 15), Color(80, 200, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "dusk" -> mixColors(Color(40, 15, 50), Color(220, 100, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "aurora" -> mixColors(Color(10, 15, 40), Color(80, 255, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "retrowave" -> mixColors(Color(30, 0, 50), Color(255, 0, 110), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "y2k" -> mixColors(Color(180, 120, 255), Color(255, 200, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "dustyrose" -> mixColors(Color(180, 140, 150), Color(220, 190, 195), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sage" -> mixColors(Color(140, 160, 140), Color(190, 210, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cloudburst" -> mixColors(Color(150, 160, 180), Color(200, 210, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "monolith" -> mixColors(Color(0, 0, 0), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "bloodline" -> mixColors(Color(40, 0, 0), Color(200, 20, 20), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "lavender" -> mixColors(Color(200, 180, 220), Color(230, 220, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "butter" -> mixColors(Color(240, 220, 170), Color(255, 245, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gothic" -> mixColors(Color(8, 8, 12), Color(60, 10, 20), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "phantom" -> mixColors(Color(20, 25, 40), Color(100, 120, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quicksilver" -> mixColors(Color(180, 190, 200), Color(220, 230, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mercury" -> mixColors(Color(40, 30, 60), Color(190, 200, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tropical" -> mixColors(Color(0, 80, 100), Color(255, 220, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mango" -> mixColors(Color(100, 30, 60), Color(255, 150, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rust" -> mixColors(Color(60, 30, 20), Color(180, 80, 30), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "concrete" -> mixColors(Color(90, 90, 90), Color(130, 130, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "nebula" -> mixColors(Color(5, 0, 15), Color(120, 50, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "supernova" -> mixColors(Color(10, 10, 30), Color(100, 150, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "eclipse" -> mixColors(Color(15, 5, 25), Color(80, 0, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "iceberg" -> mixColors(Color(150, 200, 220), Color(220, 240, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "scarlet" -> mixColors(Color(80, 10, 20), Color(230, 50, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cyberpink" -> mixColors(Color(20, 0, 30), Color(255, 50, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "matrix" -> mixColors(Color(0, 10, 0), Color(0, 180, 0), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "solarglare" -> mixColors(Color(40, 30, 0), Color(255, 200, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "emerald" -> mixColors(Color(0, 80, 40), Color(80, 200, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sapphire" -> mixColors(Color(0, 30, 80), Color(50, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "ruby" -> mixColors(Color(80, 10, 20), Color(220, 30, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "topaz" -> mixColors(Color(100, 60, 0), Color(255, 180, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "amethyst2" -> mixColors(Color(60, 20, 100), Color(180, 100, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "jade" -> mixColors(Color(0, 80, 50), Color(100, 180, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "opal" -> mixColors(Color(150, 100, 150), Color(200, 180, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "garnet" -> mixColors(Color(60, 10, 20), Color(180, 40, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "turquoise" -> mixColors(Color(0, 80, 100), Color(50, 200, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "citrine" -> mixColors(Color(120, 80, 0), Color(255, 200, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "peridot" -> mixColors(Color(80, 100, 0), Color(200, 220, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "aquamarine" -> mixColors(Color(0, 100, 120), Color(100, 220, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tanzanite" -> mixColors(Color(40, 20, 100), Color(120, 80, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "morganite" -> mixColors(Color(150, 100, 120), Color(255, 180, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "kunzite" -> mixColors(Color(150, 100, 150), Color(255, 180, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "spinel" -> mixColors(Color(80, 40, 100), Color(200, 120, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "zircon" -> mixColors(Color(60, 80, 120), Color(150, 180, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tourmaline" -> mixColors(Color(0, 100, 80), Color(100, 200, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "alexandrite" -> mixColors(Color(80, 40, 100), Color(100, 150, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "iolite" -> mixColors(Color(40, 40, 100), Color(120, 100, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chrysoberyl" -> mixColors(Color(100, 100, 0), Color(220, 200, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "beryl" -> mixColors(Color(0, 100, 80), Color(80, 200, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "corundum" -> mixColors(Color(80, 20, 40), Color(200, 80, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "beryl2" -> mixColors(Color(60, 120, 80), Color(120, 200, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quartz" -> mixColors(Color(180, 180, 200), Color(220, 220, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "moonstone" -> mixColors(Color(180, 180, 200), Color(240, 240, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sunstone" -> mixColors(Color(150, 80, 30), Color(255, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "labradorite" -> mixColors(Color(40, 50, 70), Color(120, 140, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "spectrolite" -> mixColors(Color(30, 40, 60), Color(100, 120, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "apatite" -> mixColors(Color(0, 80, 120), Color(80, 180, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fluorite" -> mixColors(Color(80, 60, 120), Color(180, 140, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "calcite" -> mixColors(Color(200, 200, 180), Color(255, 255, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sodalite" -> mixColors(Color(20, 40, 100), Color(60, 100, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "lapis" -> mixColors(Color(20, 30, 100), Color(60, 80, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "malachite" -> mixColors(Color(0, 80, 40), Color(50, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "azurite" -> mixColors(Color(0, 40, 100), Color(50, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhodochrosite" -> mixColors(Color(150, 80, 100), Color(255, 150, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhodonite" -> mixColors(Color(100, 50, 60), Color(200, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "serpentine" -> mixColors(Color(40, 80, 40), Color(100, 160, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "serpentine2" -> mixColors(Color(60, 100, 50), Color(120, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "howlite" -> mixColors(Color(200, 200, 210), Color(240, 240, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "obsidian2" -> mixColors(Color(20, 15, 25), Color(60, 50, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "onyx" -> mixColors(Color(20, 20, 25), Color(80, 80, 90), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "jasper" -> mixColors(Color(120, 60, 40), Color(200, 120, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "agate" -> mixColors(Color(150, 100, 120), Color(220, 180, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chert" -> mixColors(Color(80, 80, 90), Color(150, 150, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "flint" -> mixColors(Color(50, 50, 60), Color(120, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chert2" -> mixColors(Color(60, 70, 80), Color(130, 140, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "basalt" -> mixColors(Color(40, 40, 50), Color(100, 100, 110), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "granite" -> mixColors(Color(150, 120, 100), Color(220, 190, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "marble" -> mixColors(Color(200, 200, 210), Color(250, 250, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sandstone" -> mixColors(Color(180, 150, 100), Color(240, 210, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "limestone" -> mixColors(Color(180, 180, 170), Color(240, 240, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "shale" -> mixColors(Color(60, 60, 70), Color(120, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "slate2" -> mixColors(Color(50, 55, 65), Color(100, 110, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quartzite" -> mixColors(Color(200, 200, 210), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gneiss" -> mixColors(Color(100, 90, 80), Color(180, 170, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "schist" -> mixColors(Color(80, 80, 70), Color(150, 150, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "diorite" -> mixColors(Color(180, 180, 190), Color(240, 240, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "andesite" -> mixColors(Color(100, 100, 100), Color(170, 170, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhyolite" -> mixColors(Color(150, 130, 120), Color(220, 200, 190), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pumice" -> mixColors(Color(150, 140, 130), Color(220, 210, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "scoria" -> mixColors(Color(60, 40, 40), Color(120, 90, 90), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tuff" -> mixColors(Color(100, 100, 100), Color(160, 160, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "dolomite" -> mixColors(Color(180, 180, 170), Color(240, 240, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "halite" -> mixColors(Color(200, 220, 240), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gypsum" -> mixColors(Color(200, 200, 190), Color(255, 255, 245), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "anhydrite" -> mixColors(Color(180, 180, 200), Color(230, 230, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "barite" -> mixColors(Color(200, 200, 220), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "celestine" -> mixColors(Color(150, 180, 220), Color(200, 220, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "anglesite" -> mixColors(Color(200, 200, 180), Color(255, 255, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "galena" -> mixColors(Color(60, 60, 70), Color(120, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sphalerite" -> mixColors(Color(80, 60, 30), Color(180, 140, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cinnabar" -> mixColors(Color(100, 20, 20), Color(220, 50, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "stibnite" -> mixColors(Color(50, 50, 60), Color(120, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "orpiment" -> mixColors(Color(180, 140, 0), Color(255, 200, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "realgar" -> mixColors(Color(120, 40, 0), Color(220, 80, 20), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "copper" -> mixColors(Color(120, 60, 30), Color(220, 130, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "silver" -> mixColors(Color(150, 150, 160), Color(220, 220, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gold" -> mixColors(Color(120, 90, 0), Color(255, 200, 50), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "platinum" -> mixColors(Color(160, 160, 170), Color(230, 230, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "titanium" -> mixColors(Color(140, 140, 150), Color(200, 200, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cobalt" -> mixColors(Color(40, 50, 100), Color(80, 100, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "nickel" -> mixColors(Color(100, 110, 100), Color(180, 190, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chromium" -> mixColors(Color(60, 80, 60), Color(120, 160, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "manganese" -> mixColors(Color(80, 60, 80), Color(160, 120, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "vanadium" -> mixColors(Color(60, 60, 80), Color(120, 120, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "molybdenum" -> mixColors(Color(70, 70, 80), Color(140, 140, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tungsten" -> mixColors(Color(60, 60, 60), Color(130, 130, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "ocean" -> mixColors(Color(0, 50, 100), Color(0, 150, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sunset" -> mixColors(Color(255, 100, 50), Color(255, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "forest" -> mixColors(Color(20, 60, 30), Color(60, 140, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "midnight" -> mixColors(Color(10, 10, 40), Color(40, 40, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cherry" -> mixColors(Color(200, 50, 80), Color(255, 150, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "minty" -> mixColors(Color(100, 200, 150), Color(180, 255, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "coralreef" -> mixColors(Color(255, 100, 120), Color(255, 180, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "thunder" -> mixColors(Color(50, 0, 100), Color(150, 100, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "honey" -> mixColors(Color(200, 150, 50), Color(255, 220, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "ice" -> mixColors(Color(150, 200, 255), Color(220, 240, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "velvet" -> mixColors(Color(100, 20, 40), Color(180, 60, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "moss" -> mixColors(Color(50, 70, 40), Color(100, 130, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "plum" -> mixColors(Color(80, 40, 100), Color(150, 80, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sand" -> mixColors(Color(180, 150, 100), Color(230, 200, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "storm" -> mixColors(Color(50, 50, 70), Color(100, 100, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "peach" -> mixColors(Color(255, 180, 150), Color(255, 220, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "denim" -> mixColors(Color(50, 80, 140), Color(100, 140, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "olive" -> mixColors(Color(100, 100, 50), Color(150, 150, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "wine" -> mixColors(Color(80, 20, 40), Color(150, 50, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sky" -> mixColors(Color(100, 180, 255), Color(180, 220, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "amber" -> mixColors(Color(200, 120, 50), Color(255, 180, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fern" -> mixColors(Color(60, 120, 60), Color(120, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "iris" -> mixColors(Color(100, 80, 160), Color(180, 140, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "crimson" -> mixColors(Color(150, 20, 40), Color(220, 50, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "indigo" -> mixColors(Color(50, 50, 150), Color(100, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "magenta" -> mixColors(Color(180, 50, 150), Color(255, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "ochre" -> mixColors(Color(180, 120, 50), Color(220, 160, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sienna" -> mixColors(Color(150, 70, 40), Color(200, 110, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "violet" -> mixColors(Color(100, 50, 150), Color(180, 100, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chartreuse" -> mixColors(Color(150, 200, 50), Color(200, 255, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fuchsia" -> mixColors(Color(180, 50, 150), Color(255, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "lime" -> mixColors(Color(100, 200, 50), Color(150, 255, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "navy" -> mixColors(Color(20, 30, 80), Color(50, 70, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "salmon" -> mixColors(Color(250, 128, 114), Color(255, 180, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tan" -> mixColors(Color(180, 150, 120), Color(220, 190, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "khaki" -> mixColors(Color(180, 170, 100), Color(230, 220, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "maroon" -> mixColors(Color(100, 20, 40), Color(160, 50, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "teal" -> mixColors(Color(0, 128, 128), Color(80, 200, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cyan" -> mixColors(Color(0, 180, 200), Color(100, 230, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "bronze" -> mixColors(Color(140, 100, 50), Color(200, 150, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pearl" -> mixColors(Color(220, 210, 200), Color(250, 245, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "mahogany" -> mixColors(Color(120, 40, 40), Color(180, 80, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "jade2" -> mixColors(Color(0, 100, 80), Color(80, 180, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cobalt2" -> mixColors(Color(30, 60, 120), Color(60, 100, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "emerald2" -> mixColors(Color(0, 120, 80), Color(80, 200, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sapphire2" -> mixColors(Color(20, 50, 120), Color(60, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "ruby2" -> mixColors(Color(120, 20, 40), Color(200, 50, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "topaz2" -> mixColors(Color(150, 100, 50), Color(230, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "amethyst3" -> mixColors(Color(80, 40, 120), Color(160, 100, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "aquamarine2" -> mixColors(Color(50, 150, 140), Color(120, 220, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "garnet2" -> mixColors(Color(100, 30, 40), Color(180, 60, 70), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "opal2" -> mixColors(Color(180, 150, 180), Color(230, 200, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tourmaline2" -> mixColors(Color(30, 120, 100), Color(80, 200, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "zircon2" -> mixColors(Color(80, 100, 140), Color(140, 170, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "peridot2" -> mixColors(Color(100, 130, 50), Color(180, 220, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tanzanite2" -> mixColors(Color(60, 40, 120), Color(120, 80, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "spinel2" -> mixColors(Color(100, 60, 120), Color(180, 120, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "morganite2" -> mixColors(Color(200, 140, 160), Color(255, 200, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "kunzite2" -> mixColors(Color(180, 130, 170), Color(250, 200, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "alexandrite2" -> mixColors(Color(100, 60, 120), Color(140, 120, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "iolite2" -> mixColors(Color(60, 60, 120), Color(120, 110, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chrysoberyl2" -> mixColors(Color(130, 130, 50), Color(200, 200, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "beryl3" -> mixColors(Color(50, 130, 100), Color(100, 200, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "corundum2" -> mixColors(Color(100, 40, 60), Color(180, 80, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quartz2" -> mixColors(Color(200, 200, 210), Color(240, 240, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "moonstone2" -> mixColors(Color(190, 190, 210), Color(245, 245, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sunstone2" -> mixColors(Color(180, 100, 50), Color(255, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "labradorite2" -> mixColors(Color(50, 60, 80), Color(110, 130, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "spectrolite2" -> mixColors(Color(40, 50, 70), Color(90, 110, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "apatite2" -> mixColors(Color(30, 100, 130), Color(80, 180, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "fluorite2" -> mixColors(Color(100, 80, 140), Color(190, 150, 230), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "calcite2" -> mixColors(Color(210, 210, 190), Color(255, 255, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sodalite2" -> mixColors(Color(30, 50, 110), Color(70, 100, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "lapis2" -> mixColors(Color(30, 40, 110), Color(70, 90, 190), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "malachite2" -> mixColors(Color(20, 90, 50), Color(60, 180, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "azurite2" -> mixColors(Color(20, 50, 110), Color(60, 110, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhodochrosite2" -> mixColors(Color(160, 90, 110), Color(255, 150, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhodonite2" -> mixColors(Color(110, 60, 70), Color(200, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "serpentine3" -> mixColors(Color(50, 90, 50), Color(110, 170, 90), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "howlite2" -> mixColors(Color(210, 210, 220), Color(250, 250, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "onyx2" -> mixColors(Color(30, 30, 35), Color(90, 90, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "jasper2" -> mixColors(Color(130, 70, 50), Color(210, 130, 90), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "agate2" -> mixColors(Color(160, 110, 130), Color(230, 190, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chert3" -> mixColors(Color(90, 90, 100), Color(160, 160, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "flint2" -> mixColors(Color(60, 60, 70), Color(130, 130, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "basalt2" -> mixColors(Color(50, 50, 60), Color(110, 110, 120), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "granite2" -> mixColors(Color(160, 130, 110), Color(230, 200, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "marble2" -> mixColors(Color(210, 210, 220), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sandstone2" -> mixColors(Color(190, 160, 110), Color(250, 220, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "limestone2" -> mixColors(Color(190, 190, 180), Color(250, 250, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "shale2" -> mixColors(Color(70, 70, 80), Color(130, 130, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "slate3" -> mixColors(Color(60, 65, 75), Color(110, 120, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "quartzite2" -> mixColors(Color(210, 210, 220), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gneiss2" -> mixColors(Color(110, 100, 90), Color(190, 180, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "schist2" -> mixColors(Color(90, 90, 80), Color(160, 160, 150), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "diorite2" -> mixColors(Color(190, 190, 200), Color(250, 250, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "andesite2" -> mixColors(Color(110, 110, 110), Color(180, 180, 180), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "rhyolite2" -> mixColors(Color(160, 140, 130), Color(230, 210, 200), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "pumice2" -> mixColors(Color(160, 150, 140), Color(230, 220, 210), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "scoria2" -> mixColors(Color(70, 50, 50), Color(130, 100, 100), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tuff2" -> mixColors(Color(110, 110, 110), Color(170, 170, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "dolomite2" -> mixColors(Color(190, 190, 180), Color(250, 250, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "halite2" -> mixColors(Color(210, 230, 250), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gypsum2" -> mixColors(Color(210, 210, 200), Color(255, 255, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "anhydrite2" -> mixColors(Color(190, 190, 210), Color(240, 240, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "barite2" -> mixColors(Color(210, 210, 230), Color(255, 255, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "celestine2" -> mixColors(Color(160, 190, 230), Color(210, 230, 255), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "anglesite2" -> mixColors(Color(210, 210, 190), Color(255, 255, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "galena2" -> mixColors(Color(70, 70, 80), Color(130, 130, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "sphalerite2" -> mixColors(Color(90, 70, 40), Color(190, 150, 90), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cinnabar2" -> mixColors(Color(110, 30, 30), Color(230, 60, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "stibnite2" -> mixColors(Color(60, 60, 70), Color(130, 130, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "orpiment2" -> mixColors(Color(190, 150, 10), Color(255, 210, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "realgar2" -> mixColors(Color(130, 50, 10), Color(230, 90, 30), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "copper2" -> mixColors(Color(130, 70, 40), Color(230, 140, 80), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "silver2" -> mixColors(Color(160, 160, 170), Color(230, 230, 240), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "gold2" -> mixColors(Color(130, 100, 10), Color(255, 210, 60), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "platinum2" -> mixColors(Color(170, 170, 180), Color(240, 240, 250), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "titanium2" -> mixColors(Color(150, 150, 160), Color(210, 210, 220), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "cobalt3" -> mixColors(Color(50, 60, 110), Color(90, 110, 190), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "nickel2" -> mixColors(Color(110, 120, 110), Color(190, 200, 190), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "chromium2" -> mixColors(Color(70, 90, 70), Color(130, 170, 130), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "manganese2" -> mixColors(Color(90, 70, 90), Color(170, 130, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "vanadium2" -> mixColors(Color(70, 70, 90), Color(130, 130, 170), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "molybdenum2" -> mixColors(Color(80, 80, 90), Color(150, 150, 160), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "tungsten2" -> mixColors(Color(70, 70, 70), Color(140, 140, 140), fadeSpeed, index).let { Color(it.red, it.green, it.blue, alpha) }
            "astolfo" -> ColorUtils.skyRainbow(index, 0.6F, 1F, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }
            "rainbow" -> ColorUtils.skyRainbow(index, 1F, 1F, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue, alpha) }
            else -> Color(-1)
        }
    }

    fun getColorForMode(mode: String, index: Int = 0): Color {
        val fadeVal = ThemeFadeSpeed / 5.0 * if (updown) 1 else -1
        if (mode.startsWith("#")) {
            return parseHexColor(mode)
        }
        
        when (mode.lowercase()) {
            "astolfo" -> return ColorUtils.skyRainbow(index, 0.6F, 1F, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue) }
            "rainbow" -> return ColorUtils.skyRainbow(index, 1F, 1F, 20000F / ThemeFadeSpeed).let { Color(it.red, it.green, it.blue) }
        }
        
        val colorPair = themeColorMap[mode.lowercase()] ?: return Color(-1)
        return mixColors(colorPair.first, colorPair.second, fadeVal, index)
    }

    fun getThemeColorPair(mode: String): Pair<Color, Color>? {
        if (mode.startsWith("#")) {
            val color = parseHexColor(mode)
            return Pair(color, color)
        }
        
        when (mode.lowercase()) {
            "astolfo" -> return Pair(
                ColorUtils.skyRainbow(0, 0.6F, 1F, 20000F / ThemeFadeSpeed),
                ColorUtils.skyRainbow(90, 0.6F, 1F, 20000F / ThemeFadeSpeed)
            )
            "rainbow" -> return Pair(
                ColorUtils.skyRainbow(0, 1F, 1F, 20000F / ThemeFadeSpeed),
                ColorUtils.skyRainbow(90, 1F, 1F, 20000F / ThemeFadeSpeed)
            )
        }
        
        return themeColorMap[mode.lowercase()]
    }
}
