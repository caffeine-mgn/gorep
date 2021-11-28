package pw.binom.gorep

import pw.binom.Input
import pw.binom.concurrency.Worker
import pw.binom.concurrency.create
import pw.binom.io.Closeable
import pw.binom.io.bufferedReader
import pw.binom.io.bufferedWriter
import pw.binom.io.use
import pw.binom.process.Process
import pw.binom.process.execute

object ProcessRunner {
    fun execute(
        path: String,
        args: List<String>,
        env: Map<String, String>,
        workDir: String?,
        stderr: Boolean,
        stdout: Boolean,
        stdin: String?,
        background: Boolean,
    ): String? {
        val process = Process.execute(
            path = path,
            args = args,
            env = env,
            workDir = workDir,
        )
        if (stdin != null) {
            process.stdin.bufferedWriter(closeParent = false).use {
                it.append(stdin)
            }
        }
        if (background) {
            return null
        }

        if (stderr || stdout) {
            val sb = StringBuilder()
            val errReader = if (stderr) {
                ParallelStreamReader(process = process, input = process.stderr, appendable = sb)
            } else {
                null
            }

            val outReader = if (stderr) {
                ParallelStreamReader(process = process, input = process.stdout, appendable = sb)
            } else {
                null
            }
            process.join()
            errReader?.close()
            outReader?.close()
            return sb.toString()
        }
        process.join()
        return null
    }
}

class ParallelStreamReader(val process: Process, val input: Input, val appendable: Appendable) : Closeable {
    private val worker = Worker.create()
    override fun close() {
        worker.requestTermination()
    }

    init {
        worker.execute {
            input.bufferedReader().use {
                while (process.isActive) {
                    appendable.appendLine(it.readln())
                }
            }
        }
    }
}