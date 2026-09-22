package com.megamind.yanguservice.data.telephony

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSmsGateway(context: Context) : SmsGateway {
    private val appContext = context.applicationContext
    private val nextRequestCode = AtomicInteger(1)

    override suspend fun send(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Autorisation SMS manquante")
        }
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            throw UnsupportedOperationException("Ce téléphone ne peut pas envoyer de SMS")
        }

        val subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            throw IllegalStateException("Sélectionnez une SIM par défaut pour les SMS")
        }
        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
        }
        val parts = manager.divideMessage(message)
        if (parts.isEmpty()) throw IllegalArgumentException("Le SMS est vide")

        try {
            withTimeout(60_000) {
                awaitSentResult(manager, phoneNumber, parts)
            }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            throw IllegalStateException("Confirmation d'envoi SMS indisponible", error)
        }
    }

    private suspend fun awaitSentResult(
        manager: SmsManager,
        phoneNumber: String,
        parts: ArrayList<String>
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        val action = "${appContext.packageName}.SMS_SENT.${nextRequestCode.getAndIncrement()}"
        val registered = AtomicBoolean(false)
        var remaining = parts.size
        var firstFailure: Int? = null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!continuation.isActive) return
                if (resultCode != Activity.RESULT_OK && firstFailure == null) {
                    firstFailure = resultCode
                }
                remaining--
                if (remaining == 0) {
                    if (registered.compareAndSet(true, false)) {
                        appContext.unregisterReceiver(this)
                    }
                    val failure = firstFailure
                    if (failure == null) continuation.resume(Unit)
                    else continuation.resumeWithException(
                        IllegalStateException("Échec de l'envoi SMS (code $failure)")
                    )
                }
            }
        }

        try {
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            registered.set(true)
            continuation.invokeOnCancellation {
                if (registered.compareAndSet(true, false)) appContext.unregisterReceiver(receiver)
            }
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            repeat(parts.size) {
                sentIntents += PendingIntent.getBroadcast(
                    appContext,
                    nextRequestCode.getAndIncrement(),
                    Intent(action).setPackage(appContext.packageName),
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            if (parts.size == 1) {
                manager.sendTextMessage(phoneNumber, null, parts[0], sentIntents[0], null)
            } else {
                manager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
            }
        } catch (error: Exception) {
            if (registered.compareAndSet(true, false)) appContext.unregisterReceiver(receiver)
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}
