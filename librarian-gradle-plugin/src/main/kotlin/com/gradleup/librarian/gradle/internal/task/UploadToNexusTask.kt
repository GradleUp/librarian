package com.gradleup.librarian.gradle.internal.task

import kotlinx.coroutines.runBlocking
import net.mbonnin.vespene.lib.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException

abstract class UploadToNexusTask : DefaultTask() {
  @get:Input
  abstract val username: Property<String>

  @get:Input
  abstract val password: Property<String>

  @get:Input
  abstract val url: Property<String>

  @get:InputFiles
  abstract val files: ConfigurableFileCollection

  @TaskAction
  fun taskAction() {
    runBlocking {
      uploadNexus(logger, url.get(), username.get(), password.get(), files)
    }
  }
}

internal class InputFile(val file: File, val path: String)

internal fun FileCollection.toInputFiles(): List<InputFile> {
  val inputFiles = mutableListOf<InputFile>()

  asFileTree.visit {
    if (it.file.isFile) {
      inputFiles.add(InputFile(it.file, it.path))
    }
  }

  return inputFiles
}

private fun uploadNexus(
    logger: Logger,
    url: String,
    username: String,
    password: String,
    files: FileCollection,
) {
  val okHttpClient = OkHttpClient(username, password)
  files.toInputFiles().forEachIndexed { index, inputFile ->
    val fileUrl = buildString {
      append(url)
      if (!url.endsWith('/')) {
        append("/")
      }
      append(inputFile.path)
    }
    // TDOD: add progress
    // See https://github.com/gradle/gradle/issues/3654
    logger.info("Uploading $fileUrl")

    val request = Request.Builder()
        .put(inputFile.file.asRequestBody("application/octet-stream".toMediaType()))
        .url(fileUrl)
        .build()

    try {
      val uploadResponse = okHttpClient.newCall(request).execute()
      check(uploadResponse.isSuccessful) {
        "Cannot put $fileUrl (${uploadResponse.code}):\n${uploadResponse.body?.string()}"
      }
    } catch (e: IOException) {
      throw Exception("Error while uploading $fileUrl", e)
    }
  }
}