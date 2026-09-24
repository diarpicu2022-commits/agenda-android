package com.dpinta.agenda.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Entrega la contraseña de la base de datos cifrada.
 *
 * La contraseña son 32 bytes aleatorios que solo existen en disco envueltos con una
 * clave AES-GCM del Android Keystore, que nunca sale del hardware seguro. El archivo
 * vive en noBackupFilesDir, así que tampoco viaja en copias de seguridad.
 */
class DatabaseKeyManager(context: Context) {

    private val file = File(context.noBackupFilesDir, WRAPPED_FILE)

    @Synchronized
    fun passphrase(): ByteArray =
        if (file.exists()) unwrap(file.readBytes()) else create()

    private fun create(): ByteArray {
        val secret = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, keystoreKey()) }
        val iv = cipher.iv
        val sealed = cipher.doFinal(secret)
        // Formato: [longitud IV][IV][texto cifrado + etiqueta GCM]
        file.writeBytes(byteArrayOf(iv.size.toByte()) + iv + sealed)
        return secret
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val ivSize = blob[0].toInt()
        val iv = blob.copyOfRange(1, 1 + ivSize)
        val sealed = blob.copyOfRange(1 + ivSize, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(sealed)
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "agenda_db_key"
        const val WRAPPED_FILE = "db.key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
    }
}
