package com.gradleup.librarian.gradle.internal.task

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.Timeout
import okio.buffer
import okio.use
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.sin

abstract class DeployToPortalTask : DefaultTask() {
  @get:Input
  @get:Optional
  abstract val username: Property<String>

  @get:Input
  @get:Optional
  abstract val password: Property<String>

  @get:Input
  abstract val version: Property<String>

  @get:Input
  abstract val baseUrl: Property<String>

  @get:Input
  abstract val deploymentDescription: Property<String>

  @get:InputFiles
  abstract val files: ConfigurableFileCollection

  @get:Input
  abstract val automatic: Property<Boolean>

  @TaskAction
  fun taskAction() {
    if (!username.isPresent) {
      error("No sonatype username found, double check your LIBRARIAN_SONATYPE_USERNAME environment variable")
    }
    if (!password.isPresent) {
      error("No sonatype password found, double check your LIBRARIAN_SONATYPE_PASSWORD environment variable")
    }

    val username = username.get()
    val password = password.get()

    check(version.get().endsWith("-SNAPSHOT").not()) {
      "Cannot deploy a SNAPSHOT version ('${version.get()}') to the central portal)."
    }
    val token = "$username:$password".let {
      Buffer().writeUtf8(it).readByteString().base64()
    }

    val body = MultipartBody.Builder()
        .addFormDataPart(
            "bundle",
            deploymentDescription.get(),
            MyBody(files)
        )
        .build()

    val publicationType = if (automatic.get()) "AUTOMATIC" else "USER_MANAGED"

    Request.Builder()
        .post(body)
        .addHeader("Authorization", "UserToken $token")
        .url("${baseUrl.get()}/api/v1/publisher/upload?publishingType=$publicationType")
        .build()
        .let {
          try {
            OkHttpClient.Builder()
                .build()
                .newCall(it).execute()
          } catch (e: IOException) {
            throw Exception("Cannot publish to maven central", e)
          }
        }.use {
          if (!it.isSuccessful) {
            error("Cannot publish to maven central (status='${it.code}'): ${it.body?.string()}")
          }
        }
  }
}

internal class MyBody(val files: FileCollection) : RequestBody() {
  override fun contentType(): MediaType {
    return "application/zip".toMediaType()
  }

  override fun writeTo(sink: BufferedSink) {
    val stream = ZipOutputStream(sink.outputStream())
    files.toInputFiles().forEach {
      stream.putNextEntry(ZipEntry(it.path))
      it.file.inputStream().use {
        it.copyTo(stream)
      }
      stream.closeEntry()
    }
    stream.flush()
    sink.flush()
  }
}