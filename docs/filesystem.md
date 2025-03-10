---
artifact: "modernstorage-filesystem"
---

# FileSystem

`{{ artifact }}` is a library abstracting storage interactions when using the
[Storage Access Framework][saf_guide] by wrapping document uris in [Path][path_api]. It allows using
methods from [java.nio.Files][java.nio.Files_api] without having to learn another API like
[DocumentFile][DocumentFile_api].

[java.nio][java.nio_api] is a non-blocking I/O API, available from API 26+ (Android Oreo). Popular
libraries are already accepting [Path][path_api] as input such as Apache Commons, Google Guava and
Okio.

!!! info
    Not all [java.nio.Files][java.nio.Files_api] methods are supported **yet**. This guide
    highlights common supported ones.

## Add dependency to project

`{{ artifact }}` is available on `mavenCentral()`.

```groovy
// build.gradle
implementation("com.google.modernstorage:{{ artifact }}:{{ lib_version }}")
```

If your app `minSdk` is lower than **API 26**, you need to override the **filesystem** `minSdk`
requirement in your app's manifest:

```xml
<!-- Don't forget to add the tools namespace (xmlns:tools) -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.sample.app">

    <!-- Override API 26 minSdk -->
    <uses-sdk tools:overrideLibrary="com.google.modernstorage.filesystem" />

    <!-- ... -->
</manifest>
```

!!! info
    `{{ artifact }}` is only available for **API 26+** as Android adds support for
    [java.nio.Files][java.nio.Files_api] API from Oreo. For previous versions, you can request
    `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions to access the shared storage.

## API reference
`{{ artifact }}` API reference is available [here][api_reference].

## Initialize before usage
To interact with FileSystem, you need to initialize it first:

```kotlin
AndroidFileSystems.initialize(context)
```

## Select a file using SAF
```kotlin
/**
 * We register first an ActivityResult handler for Intent.ACTION_OPEN_DOCUMENT
 * Read more about ActivityResult here: https://developer.android.com/training/basics/intents/result
 */
val actionOpenTextFile = registerForActivityResult(OpenDocument()) { uri ->
    if(uri != null) {
        // textPath is an instance of java.nio.file.Path
        val textPath = AndroidPaths.get(documentUri)
    }
}

actionOpenTextFile.launch(arrayOf("text/*"))
```

## Get a Path from a document Uri
You can get a [Path][path_api] from a document [Uri][Uri_api] by using the method
`AndroidPaths.get`:

```kotlin
/**
 * documentUri refers to a Uri your app has received using SAF
 */
val path = AndroidPaths.get(documentUri)
```

## Get file size
You can get the file size by using the method `Files.size`:

```kotlin
val path = AndroidPaths.get(documentUri)
val size = Files.size(path)
```

## Get InputStream
You can get an [InputStream][InputStream_api] of by using the method `Files.newInputStream`:

```kotlin
val path = AndroidPaths.get(documentUri)
val inputStream = Files.newInputStream(path)
```

## Read a text file
You can read a text file by using the method `Files.readAllLines`:

```kotlin
/**
 * documentUri refers to a Uri your app has received using SAF
 */
val path = AndroidPaths.get(documentUri)
val content = Files.readAllLines(path).joinToString(separator = "\n")
```

## Get a bitmap from an image
Modifying a file requires to scan it to make MediaStore aware of the file changes (size,
modification date, etc.). To request a scan for a media URI, use the `scanUri` method:

```kotlin
val path = AndroidPaths.get(documentUri)
val inputStream = Files.newInputStream(path)
val bitmap = BitmapFactory.decodeStream(inputStream)
```

[saf_guide]: https://developer.android.com/guide/topics/providers/document-provider
[path_api]: https://developer.android.com/reference/java/nio/file/Path
[java.nio.Files_api]: https://developer.android.com/reference/kotlin/java/nio/file/Files
[DocumentFile_api]: https://developer.android.com/reference/kotlin/androidx/documentfile/provider/DocumentFile
[java.nio_api]: https://developer.android.com/reference/java/nio/package-summary
[Uri_api]: https://developer.android.com/reference/kotlin/android/net/Uri
[api_reference]: /modernstorage/api/filesystem/
[InputStream_api]: https://developer.android.com/reference/kotlin/java/io/InputStream
