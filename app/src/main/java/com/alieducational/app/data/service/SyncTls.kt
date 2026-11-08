package com.alieducational.app.data.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.math.BigInteger
import java.net.Socket
import java.util.Date
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal

/** Pair by comparing the Owner certificate fingerprint in person. Never trust arbitrary certificates. */
object SyncTls {
    private const val ALIAS = "ali-owner-sync-ec-v2"
    @Synchronized private fun store(): KeyStore {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(ALIAS)) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            generator.initialize(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                .setCertificateSubject(X500Principal("CN=Ali Educational Owner"))
                .setCertificateSerialNumber(BigInteger.ONE)
                .setCertificateNotBefore(Date(System.currentTimeMillis() - 86400000))
                .setCertificateNotAfter(Date(System.currentTimeMillis() + 10L * 365 * 86400000)).build())
            generator.generateKeyPair()
        }
        return ks
    }
    private fun fingerprint(cert: X509Certificate) = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }
    fun fingerprint(): String = fingerprint(store().getCertificate(ALIAS) as X509Certificate)
    fun serverFactory(): SSLServerSocketFactory {
        val ks = store()
        val manager = object : X509ExtendedKeyManager() {
            override fun getPrivateKey(alias: String?) = ks.getKey(ALIAS, null) as PrivateKey
            override fun getCertificateChain(alias: String?) = arrayOf(ks.getCertificate(ALIAS) as X509Certificate)
            override fun getClientAliases(type: String?, issuers: Array<java.security.Principal>?) = null
            override fun chooseClientAlias(types: Array<String>?, issuers: Array<java.security.Principal>?, socket: Socket?) = null
            override fun getServerAliases(type: String?, issuers: Array<java.security.Principal>?) = if (type == "EC") arrayOf(ALIAS) else null
            override fun chooseServerAlias(type: String?, issuers: Array<java.security.Principal>?, socket: Socket?) = if (type == "EC") ALIAS else null
        }
        return SSLContext.getInstance("TLS").apply { init(arrayOf(manager), null, null) }.serverSocketFactory
    }
    fun configure(connection: HttpsURLConnection, pin: String) {
        val expected = pin.replace(":", "").trim().lowercase()
        require(expected.matches(Regex("[0-9a-f]{64}"))) { "Pair using the Owner certificate fingerprint first" }
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>?, type: String?) { throw java.security.cert.CertificateException("No client certificates") }
            override fun checkServerTrusted(chain: Array<X509Certificate>?, type: String?) {
                val certificate = chain?.firstOrNull() ?: throw java.security.cert.CertificateException("Missing certificate")
                certificate.checkValidity()
                if (!MessageDigest.isEqual(expected.toByteArray(), fingerprint(certificate).toByteArray())) throw java.security.cert.CertificateException("Owner identity changed; pair again in person")
            }
        }
        connection.sslSocketFactory = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }.socketFactory
        // The identity is the out-of-band pinned certificate, not a mutable local IP/DNS name.
        connection.hostnameVerifier = HostnameVerifier { _, session ->
            val cert = session.peerCertificates.firstOrNull() as? X509Certificate
            cert != null && fingerprint(cert) == expected
        }
    }
}
