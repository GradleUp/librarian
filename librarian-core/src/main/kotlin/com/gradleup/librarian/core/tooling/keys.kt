package com.gradleup.librarian.core.tooling

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.Features
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec
import org.bouncycastle.openpgp.PGPEncryptedData
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.util.Date
import kotlin.io.path.readText


class SecretKeyRing(
    private val pgpSecretKeyRing: PGPSecretKeyRing,
) {
  fun secretKey(): ByteArray = pgpSecretKeyRing.secretKey.encoded
  fun publicKey(): ByteArray = pgpSecretKeyRing.publicKey.encoded
}

fun secretKeyRingOrNull(privateKeyPath: Path): SecretKeyRing? {
  val inputStream = ByteArrayInputStream(privateKeyPath.readText().dearmored())
  inputStream.use {
    val objectFactory = BcPGPObjectFactory(it)
    while(true) {
      val next = objectFactory.nextObject()
      if (next == null) {
        return null
      }
      if (next is PGPSecretKeyRing) {
        return SecretKeyRing(next)
      }
    }
  }
}

fun secretKeyRing(name: String, email: String, password: String): SecretKeyRing {
  val keyPairGenerator = KeyPairGenerator.getInstance("EdDSA", BouncyCastleProvider()).apply {
    initialize(ECNamedCurveGenParameterSpec("ed25519"))
  }
  val signatureKeyPair = JcaPGPKeyPair(PGPPublicKey.EDDSA_LEGACY, keyPairGenerator.generateKeyPair(), Date());

  val signaturePackets = PGPSignatureSubpacketGenerator().apply {
    setKeyFlags(false, KeyFlags.SIGN_DATA or KeyFlags.CERTIFY_OTHER)
    setFeature(false, Features.FEATURE_MODIFICATION_DETECTION)
    setPreferredSymmetricAlgorithms(
        false,
        intArrayOf(
            SymmetricKeyAlgorithmTags.AES_256,
            SymmetricKeyAlgorithmTags.AES_192, SymmetricKeyAlgorithmTags.AES_128
        )
    )
    setPreferredHashAlgorithms(
        false,
        intArrayOf(
            HashAlgorithmTags.SHA256,
            HashAlgorithmTags.SHA1,
            HashAlgorithmTags.SHA384,
            HashAlgorithmTags.SHA512,
            HashAlgorithmTags.SHA224,
        )
    )
  }.generate()

  val keyRingGenerator = PGPKeyRingGenerator(
      PGPSignature.POSITIVE_CERTIFICATION,
      signatureKeyPair,
      "$name <$email>",
      BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1),
      signaturePackets,
      null,
      BcPGPContentSignerBuilder(PGPPublicKey.EDDSA_LEGACY, HashAlgorithmTags.SHA1),
      BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256), 0xc0).build(password.toCharArray())
  )

  return SecretKeyRing(keyRingGenerator.generateSecretKeyRing())
}

fun ByteArray.armored(): String {
  val byteArrayOutputStream = ByteArrayOutputStream()

  ArmoredOutputStream(byteArrayOutputStream).use {
    it.write(this)
  }
  return String(byteArrayOutputStream.toByteArray())
}

fun String.dearmored(): ByteArray {
  val byteArrayInputStream = ByteArrayInputStream(this.toByteArray())

  return ArmoredInputStream(byteArrayInputStream).use {
    it.readAllBytes()
  }
}


sealed interface UploadResult
class UploadSuccess(val url: String): UploadResult
class UploadError(val message: String): UploadResult

fun uploadKey(keyServerUrl: String, publicKey: ByteArray): UploadResult {
  val request = Request.Builder()
      .put(publicKey.toRequestBody())
      .url(keyServerUrl)
      .build()

  val response = try {
    OkHttpClient().newCall(request).execute()
  } catch (e: Exception) {
    return UploadError("I/O error while executing call to $keyServerUrl: ${e.message}")
  }

  if (!response.isSuccessful) {
    return UploadError(response.body!!.string())
  }

  return UploadSuccess(response.body!!.string())
}