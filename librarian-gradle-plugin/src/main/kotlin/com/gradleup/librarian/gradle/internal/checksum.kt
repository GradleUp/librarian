package com.gradleup.librarian.gradle.internal

import okio.ByteString.Companion.toByteString
import java.io.InputStream
import java.security.MessageDigest

internal fun InputStream.digest(name: String): String {
  val md = MessageDigest.getInstance(name)

  val scratch = ByteArray(1024)

  while (true) {
    val read = read(scratch)
    if (read == -1) {
      break
    }
    md.update(scratch, 0, read)
  }

  val digest = md.digest()

  return digest.toByteString().hex()
}

