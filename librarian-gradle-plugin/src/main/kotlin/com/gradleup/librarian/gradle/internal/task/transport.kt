package com.gradleup.librarian.gradle.internal.task

import com.google.auth.oauth2.GoogleCredentials
import gratatouille.tasks.GLogger
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okio.*
import java.io.File
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

internal interface Transport {
  fun get(path: String): BufferedSource?
  fun put(path: String, body: Content)
}

internal fun Transport.put(path: String, body: String) {
  put(path, object : Content {
    override fun writeTo(sink: BufferedSink) {
      sink.writeUtf8(body)
    }
  })
}

internal fun Transport.put(path: String, body: ByteArray) {
  put(path, object : Content {
    override fun writeTo(sink: BufferedSink) {
      sink.write(body)
    }
  })
}

internal fun Transport.put(path: String, body: File) {
  put(path, object : Content {
    override fun writeTo(sink: BufferedSink) {
      body.source().use {
        sink.writeAll(it)
      }
    }
  })
}

interface Content {
  fun writeTo(sink: BufferedSink)
}

internal class NmcpCredentials(val username: String, val password: String)

internal class HttpTransport(
  baseUrl: String,
  private val credentials: NmcpCredentials?,
  private val logger: GLogger,
) : Transport {
  private val client = com.gradleup.librarian.gradle.internal.task.client.newBuilder()
    .addInterceptor { chain ->
      val builder = chain.request().newBuilder()
      if (credentials != null) {
        builder.addHeader(
          "Authorization",
          Credentials.basic(credentials.username, credentials.password),
        )
      }
      builder.addHeader("Accept", "application/json")
      builder.addHeader("User-Agent", "nmcp")
      chain.proceed(builder.build())
    }
    .build()

  private val baseUrl = baseUrl.toHttpUrl()

  override fun get(path: String): BufferedSource? {
    val url = baseUrl.newBuilder()
      .addPathSegments(path)
      .build()

    logger.lifecycle("Librarian: get '$url'")

    val response = Request.Builder()
      .get()
      .url(url)
      .build()
      .let {
        client.newCall(it).execute()
      }

    if (response.code == 404) {
      return null
    }
    check(response.isSuccessful) {
      "Librarian: cannot GET '$url' (statusCode=${response.code}):\n${response.body!!.string()}"
    }

    return response.body!!.source()
  }

  override fun put(path: String, body: Content) {
    val url = baseUrl.newBuilder()
      .addPathSegments(path)
      .build()

    logger.lifecycle("Librarian: put '$url'")

    val response = Request.Builder()
      .put(body.toRequestBody())
      .url(url)
      .build()
      .let {
        client.newCall(it).execute()
      }

    check(response.isSuccessful) {
      "Librarian: cannot PUT '$url' (statusCode=${response.code}):\n${response.body!!.string()}"
    }
  }
}

private fun Content.toRequestBody(): RequestBody {
  return object : RequestBody() {
    override fun contentType(): MediaType {
      return "application/octet-stream".toMediaType()
    }

    override fun writeTo(sink: BufferedSink) {
      this@toRequestBody.writeTo(sink)
    }
  }
}

internal class FilesystemTransport(
  private val basePath: String,
) : Transport {
  override fun get(path: String): BufferedSource? {
    val file = File(basePath).resolve(path)
    if (!file.exists()) {
      return null
    }
    return file.source().buffer()
  }

  override fun put(path: String, body: Content) {
    File(basePath).resolve(path).apply {
      parentFile.mkdirs()
      sink().buffer().use {
        body.writeTo(it)
      }
    }
  }
}

internal class GcsTransport(
  private val logger: GLogger,
  url: String,
  googleServicesJson: String,
) : Transport {
  // This uses the Gradle internal GoogleCredential which isn't great but also saves adding yet another dependency
  @Suppress("DEPRECATION")
  val credentials = GoogleCredentials.fromStream(googleServicesJson.byteInputStream())
    .createScoped(setOf("https://www.googleapis.com/auth/devstorage.read_write"))

  val baseUrl: HttpUrl
  val prefix: String
  init {
    val i = url.indexOf('/')
    val bucket: String
    if (i == -1) {
      bucket = url
      prefix = ""
    } else {
      bucket = url.substring(0, i)
      prefix = url.substring(i + 1)
    }
    baseUrl = "https://storage.googleapis.com/upload/storage/v1/b/${bucket}/o".toHttpUrl()
  }

  /**
   * https://cloud.google.com/storage/docs/json_api/v1/objects/get#response
   */
  override fun get(path: String): BufferedSource? {
    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val name = "${prefix}${path}"
    logger.lifecycle("Librarian: gcs-get $name")
    val url = baseUrl
      .newBuilder()
      .addPathSegments(name)
      .addQueryParameter("alt", "media")
      .build()
    val request = Request.Builder()
      .get()
      .addHeader("Authorization", "Bearer $accessToken")
      .url(url)
      .build()

    return executeCall(client, request)
  }

  /**
   * https://cloud.google.com/storage/docs/json_api/v1/objects/insert
   */
  override fun put(path: String, body: Content) {
    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val name = "${prefix}${path}"
    logger.lifecycle("Librarian: gcs-put $name")
    val url = baseUrl
      .newBuilder()
      .addQueryParameter("name", name)
      .addQueryParameter("uploadType", "media")
      .build()
    val request = Request.Builder()
      .post(body.toRequestBody())
      .addHeader("Authorization", "Bearer $accessToken")
      .url(url)
      .build()

    executeCall(client, request)?.close()
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
              error("Cannot POST Google Cloud object to ${request.url} (content-length: ${request.header("content-length")}) ('${response.code}'): ${response.body?.string()}")
            }
          }
        }
        continue
      }

      return response.body!!.source()
    }
  }
}