package com.megamind.yanguservice.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.megamind.yanguservice.domain.utils.AuthTokenStore
import com.megamind.yanguservice.utlis.Result
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AndroidKeystoreTokenStore(
    context: Context,
    private val dispatcher: CoroutineDispatcher
) : AuthTokenStore {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getToken(): String? {
        val encryptedToken = preferences.getString(ENCRYPTED_TOKEN_KEY, null) ?: return null
        val initializationVector = preferences.getString(INITIALIZATION_VECTOR_KEY, null)
            ?: return null

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, decode(initializationVector))
            )
            String(cipher.doFinal(decode(encryptedToken)), StandardCharsets.UTF_8)
        } catch (exception: Exception) {
            Log.e(TAG, "Impossible de déchiffrer le token API", exception)
            null
        }
    }

    override suspend fun saveToken(token: String): Result<Unit> = withContext(dispatcher) {
        try {
            require(token.isNotBlank()) { "Le token API ne peut pas être vide" }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encryptedToken = cipher.doFinal(token.trim().toByteArray(StandardCharsets.UTF_8))

            val saved = preferences.edit()
                .putString(ENCRYPTED_TOKEN_KEY, encode(encryptedToken))
                .putString(INITIALIZATION_VECTOR_KEY, encode(cipher.iv))
                .commit()

            if (saved) {
                Result.Success(Unit)
            } else {
                Result.Error(IllegalStateException("Impossible d'enregistrer le token API"))
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Impossible de chiffrer le token API", exception)
            Result.Error(exception)
        }
    }

    override suspend fun clearToken() = withContext(dispatcher) {
        preferences.edit()
            .remove(ENCRYPTED_TOKEN_KEY)
            .remove(INITIALIZATION_VECTOR_KEY)
            .commit()
        Unit
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val keySpecification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpecification)
        return keyGenerator.generateKey()
    }

    private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val TAG = "SecureTokenStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "yangu_wasender_api_key"
        const val PREFERENCES_NAME = "secure_credentials"
        const val ENCRYPTED_TOKEN_KEY = "encrypted_api_token"
        const val INITIALIZATION_VECTOR_KEY = "api_token_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
