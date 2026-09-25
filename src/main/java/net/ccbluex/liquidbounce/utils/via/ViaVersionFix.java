package net.ccbluex.liquidbounce.utils.via;

/**
 * Utility class for protocol version fix helpers.
 * Adapted from Unfair client's ViaVersion integration.
 */
public class ViaVersionFix {

    /**
     * Get the sequence number for protocol version handling.
     * Used for 1.9+ offhand slot synchronization.
     */
    public static int sequence() {
        return sequenceCounter++;
    }

    private static int sequenceCounter = 0;

    /**
     * Reset the sequence counter (called on world join).
     */
    public static void resetSequence() {
        sequenceCounter = 0;
    }
}