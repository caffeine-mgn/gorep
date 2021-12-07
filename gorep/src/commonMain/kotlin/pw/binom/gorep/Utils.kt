package pw.binom.gorep

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

fun runSuspendFunc(func: suspend CoroutineScope.() -> Unit) {
    runBlocking(block=func)
}

fun ByteArray.toHex() =
    joinToString("") {
        val str = it.toUByte().toString(16)
        if (str.length == 1) {
            "0$str"
        } else {
            str
        }
    }