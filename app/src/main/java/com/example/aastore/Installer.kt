package com.example.aastore

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import java.net.URL
import java.security.MessageDigest

object Installer {

    /** Descarga el APK y verifica su SHA-256. Llamar desde un hilo de fondo. */
    fun download(ctx: Context, e: AppEntry, onProgress: (Int) -> Unit): File {
        val dir = File(ctx.cacheDir, "apks").apply { mkdirs() }
        val file = File(dir, "${e.packageName}-${e.versionCode}.apk")
        val conn = URL(e.apkUrl).openConnection()
        val total = conn.contentLengthLong
        val md = MessageDigest.getInstance("SHA-256")
        conn.getInputStream().use { input ->
            file.outputStream().use { out ->
                val buf = ByteArray(16 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n); md.update(buf, 0, n); done += n
                    if (total > 0) onProgress((done * 100 / total).toInt())
                }
            }
        }
        val hash = md.digest().joinToString("") { "%02x".format(it) }
        if (!hash.equals(e.sha256, ignoreCase = true)) {
            file.delete()
            error("El hash del APK no coincide (descarga corrupta o alterada)")
        }
        return file
    }

    /** Instala el APK con PackageInstaller (el sistema pedirá confirmación al usuario). */
    fun install(ctx: Context, apk: File) {
        // Android exige que el usuario autorice "instalar apps desconocidas" para AAStore
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val pi = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val id = pi.createSession(params)
        pi.openSession(id).use { s ->
            apk.inputStream().use { input ->
                s.openWrite("app.apk", 0, apk.length()).use { out ->
                    input.copyTo(out); s.fsync(out)
                }
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, InstallReceiver::class.java).setPackage(ctx.packageName), flags
            )
            s.commit(pending.intentSender)
        }
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(ctx::startActivity)
            }
            else -> { /* SUCCESS u otro: aquí puedes mostrar una notificación */ }
        }
    }
}
