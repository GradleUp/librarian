package com.gradleup.librarian.gradle.internal.task

import com.google.auth.oauth2.GoogleCredentials
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GLogger
import gratatouille.tasks.GTask
import nmcp.transport.Content
import nmcp.transport.Transport
import nmcp.transport.publishFileByFile
import nmcp.transport.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import java.net.URLEncoder
import java.time.Duration
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

@GTask
fun publishToGcs(
  logger: GLogger,
  inputFiles: GInputFiles,
  bucket: String,
  prefix: String,
  credentials: String
) {
  val transport = GcsTransport(logger, bucket, prefix, credentials)

  logger.lifecycle("Uploading files to gcs://$bucket/$prefix")
  publishFileByFile(transport, inputFiles)
}

private val nmcpClient = OkHttpClient.Builder()
  .connectTimeout(Duration.ofSeconds(30))
  .writeTimeout(Duration.ofSeconds(30))
  .readTimeout(Duration.ofSeconds(60))
  .build()

internal class GcsTransport(
  private val logger: GLogger,
  bucket: String,
  private val prefix: String,
  googleServicesJson: String,
) : Transport {
  val credentials = GoogleCredentials.fromStream(googleServicesJson.byteInputStream())
    .createScoped(setOf("https://www.googleapis.com/auth/devstorage.read_write"))

  val postBaseUrl = "https://storage.googleapis.com/upload/storage/v1/b/${bucket}/o".toHttpUrl()
  val getBaseUrl = "https://storage.googleapis.com/storage/v1/b/$bucket/o".toHttpUrl()

  /**
   * https://cloud.google.com/storage/docs/json_api/v1/objects/get#response
   */
  override fun get(path: String): BufferedSource? {
    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val name = "${prefix}${path}"
    logger.info("Librarian: gcs-get $name")

    val url = getBaseUrl
      .newBuilder()
      /**
       * Send the object name as a single segment as this is required by GCP:
       * https://cloud.google.com/storage/docs/request-endpoints#encoding
       */
      .addPathSegment(name)
      .addQueryParameter("alt", "media")
      .build()
    val request = Request.Builder()
      .get()
      .addHeader("Authorization", "Bearer $accessToken")
      .url(url)
      .build()

    return executeCall(nmcpClient, request)
  }

  /**
   * https://cloud.google.com/storage/docs/json_api/v1/objects/insert
   */
  override fun put(path: String, body: Content) {
    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val name = "${prefix}${path}"
    logger.info("Librarian: gcs-put $name")
    val url = postBaseUrl
      .newBuilder()
      .addQueryParameter("name", name)
      .addQueryParameter("uploadType", "media")
      .build()
    val request = Request.Builder()
      .post(body.toRequestBody())
      .addHeader("Authorization", "Bearer $accessToken")
      .url(url)
      .build()

    executeCall(nmcpClient, request)?.close()
  }

  private fun executeCall(client: OkHttpClient, request: Request): BufferedSource? {
    var retry = 0
    while (true) {
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        response.use {
          when (response.code) {
            404 -> return null
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
              error(
                "Cannot '${request.method}' Google Cloud object to ${request.url} (content-length: ${
                  request.header(
                    "content-length"
                  )
                }) ('${response.code}'): ${response.body?.string()}"
              )
            }
          }
        }
        continue
      }

      return response.body!!.source()
    }
  }
}
