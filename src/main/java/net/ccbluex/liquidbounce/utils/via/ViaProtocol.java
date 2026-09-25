package net.ccbluex.liquidbounce.utils.via;

/**
 * Protocol version utility for Via functionality.
 * Uses simple protocol version IDs without ViaVersion dependency.
 * Reference: https://wiki.vg/Protocol_version_numbers
 */
public class ViaProtocol {

    // Common protocol version IDs
    public static final int R1_7_2 = 4;
    public static final int R1_7_6 = 5;
    public static final int R1_8 = 47;
    public static final int R1_9 = 107;
    public static final int R1_9_1 = 108;
    public static final int R1_9_2 = 109;
    public static final int R1_9_4 = 110;
    public static final int R1_10 = 210;
    public static final int R1_11 = 315;
    public static final int R1_11_1 = 316;
    public static final int R1_12 = 335;
    public static final int R1_12_1 = 338;
    public static final int R1_12_2 = 340;
    public static final int R1_13 = 393;
    public static final int R1_13_1 = 401;
    public static final int R1_13_2 = 404;
    public static final int R1_14 = 477;
    public static final int R1_14_1 = 480;
    public static final int R1_14_2 = 485;
    public static final int R1_14_3 = 490;
    public static final int R1_14_4 = 498;
    public static final int R1_15 = 573;
    public static final int R1_15_1 = 575;
    public static final int R1_15_2 = 578;
    public static final int R1_16 = 735;
    public static final int R1_16_1 = 736;
    public static final int R1_16_2 = 751;
    public static final int R1_16_3 = 753;
    public static final int R1_16_4 = 754;
    public static final int R1_17 = 755;
    public static final int R1_17_1 = 756;
    public static final int R1_18 = 757;
    public static final int R1_18_2 = 758;
    public static final int R1_19 = 759;
    public static final int R1_19_1 = 760;
    public static final int R1_19_3 = 761;
    public static final int R1_19_4 = 762;
    public static final int R1_20 = 763;
    public static final int R1_20_1 = 763;
    public static final int R1_20_2 = 764;
    public static final int R1_20_3 = 765;
    public static final int R1_20_4 = 765;
    public static final int R1_20_5 = 766;
    public static final int R1_20_6 = 766;
    public static final int R1_21 = 767;
    public static final int R1_21_1 = 767;
    public static final int R1_21_2 = 768;
    public static final int R1_21_3 = 769;
    public static final int R1_21_4 = 770;

    // Native version for this client (1.8.x)
    public static final int NATIVE_VERSION = R1_8;

    // Currently selected target protocol version
    private static int targetVersion = NATIVE_VERSION;

    /**
     * Get the target protocol version to spoof.
     */
    public static int getTargetVersion() {
        return targetVersion;
    }

    /**
     * Set the target protocol version to spoof.
     */
    public static void setTargetVersion(int version) {
        targetVersion = version;
    }

    /**
     * Check if the target version differs from native (spoofing is active).
     */
    public static boolean isSpoofing() {
        return targetVersion != NATIVE_VERSION;
    }

    // Version comparison helpers

    public static boolean isNewerThan(int version) {
        return targetVersion > version;
    }

    public static boolean isOlderThan(int version) {
        return targetVersion < version;
    }

    public static boolean isNewerThanOrEqualTo(int version) {
        return targetVersion >= version;
    }

    public static boolean isOlderThanOrEqualTo(int version) {
        return targetVersion <= version;
    }

    // Common checks

    public static boolean isNewerThan1_8() {
        return targetVersion > R1_8;
    }

    public static boolean isNewerThanOrEqualTo1_9() {
        return targetVersion >= R1_9;
    }

    public static boolean isOlderThan1_9() {
        return targetVersion < R1_9;
    }

    public static boolean isNewerThanOrEqualTo1_13() {
        return targetVersion >= R1_13;
    }

    public static boolean isOlderThan1_13() {
        return targetVersion < R1_13;
    }

    public static boolean isNewerThanOrEqualTo1_17() {
        return targetVersion >= R1_17;
    }

    /**
     * Get display name for a protocol version.
     */
    public static String getVersionName(int version) {
        switch (version) {
            case R1_7_2: return "1.7.2-1.7.5";
            case R1_7_6: return "1.7.6-1.7.10";
            case R1_8: return "1.8.x";
            case R1_9: return "1.9";
            case R1_9_1: return "1.9.1";
            case R1_9_2: return "1.9.2";
            case R1_9_4: return "1.9.3-1.9.4";
            case R1_10: return "1.10.x";
            case R1_11: return "1.11";
            case R1_11_1: return "1.11.1-1.11.2";
            case R1_12: return "1.12";
            case R1_12_1: return "1.12.1";
            case R1_12_2: return "1.12.2";
            case R1_13: return "1.13";
            case R1_13_1: return "1.13.1";
            case R1_13_2: return "1.13.2";
            case R1_14: return "1.14";
            case R1_14_1: return "1.14.1";
            case R1_14_2: return "1.14.2";
            case R1_14_3: return "1.14.3";
            case R1_14_4: return "1.14.4";
            case R1_15: return "1.15";
            case R1_15_1: return "1.15.1";
            case R1_15_2: return "1.15.2";
            case R1_16: return "1.16";
            case R1_16_1: return "1.16.1";
            case R1_16_2: return "1.16.2";
            case R1_16_3: return "1.16.3";
            case R1_16_4: return "1.16.4-1.16.5";
            case R1_17: return "1.17";
            case R1_17_1: return "1.17.1";
            case R1_18: return "1.18-1.18.1";
            case R1_18_2: return "1.18.2";
            case R1_19: return "1.19";
            case R1_19_1: return "1.19.1-1.19.2";
            case R1_19_3: return "1.19.3";
            case R1_19_4: return "1.19.4";
            case R1_20: return "1.20-1.20.1";
            case R1_20_2: return "1.20.2";
            case R1_20_3: return "1.20.3-1.20.4";
            case R1_20_5: return "1.20.5-1.20.6";
            case R1_21: return "1.21-1.21.1";
            case R1_21_2: return "1.21.2-1.21.3";
            case R1_21_4: return "1.21.4+";
            default: return "Unknown (" + version + ")";
        }
    }
}