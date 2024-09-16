package com.gradleup.librarian.gradle.internal.task

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.use
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal abstract class UploadToPortalTask : DefaultTask() {
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
        .addHeader("Authorization", "Bearer $token")
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
    if (!automatic.get()) {
      logger.log(LogLevel.LIFECYCLE, "Artifacts uploaded, go to ${baseUrl.get()} to release them")
    }
  }
}

internal class MyBody(val files: FileCollection) : RequestBody() {
  override fun contentType(): MediaType {
    return "application/octet-stream".toMediaType()
  }

  override fun writeTo(sink: BufferedSink) {
    val stream = ZipOutputStream(sink.outputStream())
    files.toInputFiles().forEach {
      // Exclude maven-metadata files or the bundle is not recognized
      // See https://slack-chats.kotlinlang.org/t/16407246/anyone-tried-the-https-central-sonatype-org-publish-publish-#c8738fe5-8051-4f64-809f-ca67a645216e
      if (it.file.name.startsWith("maven-metadata")) {
        return@forEach
      }
      stream.putNextEntry(ZipEntry(it.path))
      it.file.inputStream().use {
        it.copyTo(stream)
      }
      stream.closeEntry()
    }
    stream.finish()
    stream.flush()
    sink.flush()
  }
}