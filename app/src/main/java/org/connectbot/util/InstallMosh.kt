/*
 * Mosh support for ConnectBot
 * Copyright 2012 Daniel Drown (transport layer)
 * ConnectBot integration by bqv (Tony O)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.util

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipInputStream
import kotlin.concurrent.withLock

/**
 * Utility class to install mosh resources (terminfo database) on first launch.
 * The terminfo.zip is extracted from assets to the app's files directory.
 * This is thread-safe with a wait/notify mechanism for callers who need
 * to wait for installation to complete.
 */
object InstallMosh {
    private const val TAG = "InstallMosh"
    private const val TERMINFO_ZIP = "terminfo.zip"
    private const val TERMINFO_DIR = "terminfo"
    private const val INSTALL_MARKER = ".mosh_installed"

    private val lock = ReentrantLock()
    private val installComplete = lock.newCondition()

    @Volatile
    private var installDone = false

    @Volatile
    private var installThread: Thread? = null

    @Volatile
    private var terminfoPath: String? = null

    /**
     * Start the installation process in the background.
     * This method returns immediately. Use waitForInstall() if you need
     * to wait for installation to complete.
     *
     * @param context The application context
     */
    fun startInstall(context: Context) {
        lock.withLock {
            if (installDone || installThread != null) {
                return
            }

            installThread = Thread {
                performInstall(context.applicationContext)
            }.apply {
                name = "MoshInstaller"
                isDaemon = true
                start()
            }
        }
    }

    /**
     * Wait for the installation to complete.
     * This will block until installation is done.
     *
     * @param timeoutMs Maximum time to wait in milliseconds, or 0 for no timeout
     * @return true if installation completed successfully, false if timed out
     */
    fun waitForInstall(timeoutMs: Long = 0): Boolean {
        lock.withLock {
            if (installDone) {
                return true
            }

            return try {
                if (timeoutMs > 0) {
                    val deadline = System.currentTimeMillis() + timeoutMs
                    while (!installDone) {
                        val remaining = deadline - System.currentTimeMillis()
                        if (remaining <= 0) {
                            return false
                        }
                        installComplete.await(remaining, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                    true
                } else {
                    while (!installDone) {
                        installComplete.await()
                    }
                    true
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        }
    }

    /**
     * Check if installation is complete.
     *
     * @return true if installation has completed
     */
    fun isInstallDone(): Boolean = installDone

    /**
     * Get the path to the terminfo directory.
     * Returns null if installation is not complete.
     *
     * @return Path to terminfo directory, or null
     */
    fun getTerminfoPath(): String? = terminfoPath

    /**
     * Get the path where mosh-client binary would be located.
     * Note: The actual mosh-client is compiled into the native library,
     * this path is for reference only.
     *
     * @param context The application context
     * @return Path to the native library directory
     */
    fun getNativeLibDir(context: Context): String = context.applicationInfo.nativeLibraryDir

    private fun performInstall(context: Context) {
        try {
            val filesDir = context.filesDir
            val terminfoDir = File(filesDir, TERMINFO_DIR)
            val installMarker = File(filesDir, INSTALL_MARKER)

            // Check if already installed
            if (installMarker.exists() && terminfoDir.exists()) {
                Timber.d("Mosh resources already installed")
                terminfoPath = terminfoDir.absolutePath
                markInstallComplete()
                return
            }

            // Extract terminfo.zip from assets
            Timber.d("Installing mosh resources...")

            try {
                context.assets.open(TERMINFO_ZIP).use { assetStream ->
                    ZipInputStream(assetStream).use { zipStream ->
                        extractZip(zipStream, terminfoDir)
                    }
                }
            } catch (e: IOException) {
                // terminfo.zip might not exist if mosh native build is not configured
                Timber.w("terminfo.zip not found in assets - mosh native build may not be configured")
                // Still mark as complete so we don't keep trying
                terminfoPath = terminfoDir.absolutePath
                markInstallComplete()
                return
            }

            // Create install marker
            installMarker.createNewFile()

            terminfoPath = terminfoDir.absolutePath
            Timber.d("Mosh resources installed to: $terminfoPath")

            markInstallComplete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to install mosh resources")
            // Mark as complete anyway to avoid blocking forever
            markInstallComplete()
        }
    }

    private fun extractZip(zipStream: ZipInputStream, destDir: File) {
        destDir.mkdirs()

        var entry = zipStream.nextEntry
        while (entry != null) {
            val destFile = File(destDir, entry.name)

            // Security check: ensure the file is within destDir
            if (!destFile.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                throw SecurityException("Zip entry outside target directory: ${entry.name}")
            }

            if (entry.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { output ->
                    zipStream.copyTo(output)
                }
            }

            zipStream.closeEntry()
            entry = zipStream.nextEntry
        }
    }

    private fun markInstallComplete() {
        lock.withLock {
            installDone = true
            installThread = null
            installComplete.signalAll()
        }
    }
}
