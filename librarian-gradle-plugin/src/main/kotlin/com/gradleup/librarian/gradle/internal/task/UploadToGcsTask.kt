package com.gradleup.librarian.gradle.internal.task

import com.google.auth.oauth2.GoogleCredentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okio.use
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

internal abstract class UploadToGcsTask : DefaultTask() {
  @get:Input
  abstract val googleServicesJson: Property<String>

  @get:Input
  abstract val bucket: Property<String>

  @get:Input
  abstract val prefix: Property<String>

  @get:InputFiles
  abstract val files: ConfigurableFileCollection

  @TaskAction
  fun taskAction() {
    // This uses the Gradle internal GoogleCredential which isn't great but also saves adding yet another dependency
    @Suppress("DEPRECATION")
    val credentials = GoogleCredentials.fromStream(googleServicesJson.get().byteInputStream())
      .createScoped(setOf("https://www.googleapis.com/auth/devstorage.read_write"))

    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val client = OkHttpClient()
    val baseUrl = "https://storage.googleapis.com/upload/storage/v1/b/${bucket.get()}/o".toHttpUrl()
    files.toInputFiles().forEach {
      val body = it.file.asRequestBody()
      val name = "${prefix.get()}${it.path}"
      logger.info("Uploading $name (size: ${it.file.length()})")
      val url = baseUrl
        .newBuilder()
        .addQueryParameter("name", name)
        .addQueryParameter("uploadType", "media")
        .build()
      val request = Request.Builder()
        .post(body)
        .addHeader("Authorization", "Bearer $accessToken")
        .url(url)
        .build()

      executeCall(client, request)
    }
  }

  private fun executeCall(client: OkHttpClient, request: Request) {
    var retry = 0
    while (true) {
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          return
        }
        when (response.code) {
          in 200..299 -> {
            return
          }

          408, 429, in 500..599 -> {
            // See https://cloud.google.com/storage/docs/retry-strategy
            val delay = 2.0.pow(retry.toDouble()).seconds.inWholeMilliseconds
            Thread.sleep(delay)
            retry++
            if (retry > 5) {
              error("Too many retries ($retry), giving up ('${response.code}'): ${response.body?.string()}")
            }
          }

          else -> {
            error("Cannot POST Google Cloud object to ${request.url} (content-length: ${request.header("content-length")}) ('${response.code}'): ${response.body?.string()}")
          }
        }
      }
    }
  }
}