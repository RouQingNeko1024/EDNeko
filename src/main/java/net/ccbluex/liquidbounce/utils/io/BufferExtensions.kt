/*
 * Air Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.utils.io

import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * Prevents crashes on Java 8 where ByteBuffer.flip() returns Buffer instead of ByteBuffer.
 * Casting to Buffer first ensures compatibility across all Java versions.
 */
fun ByteBuffer.flipSafely() {
    (this as Buffer).flip()
}