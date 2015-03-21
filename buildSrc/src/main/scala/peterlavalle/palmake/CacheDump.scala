package peterlavalle.palmake

import java.io.{FileOutputStream, OutputStream, InputStream, File}
import java.net.URL
import java.util
import java.util.zip.{ZipException, ZipEntry, ZipFile}

case class CacheDump(cache: File, dump: File) {

  def copy[O <: OutputStream](inputStream: InputStream, outputStream: O): O = {

    val buffer = Array.ofDim[Byte](128)

    def recu(count: Int): Unit =
      if (count == -1) {
        outputStream.flush()
      } else {
        outputStream.write(buffer, 0, count)
        recu(inputStream.read(buffer))
      }

    recu(inputStream.read(buffer))
    outputStream
  }

  /**
   * downloads the url, to the cache, then unzips it to the dump and returns the path
   * @param url wha to download
   * @param md5 what hash we expect
   * @return file to the root of the extracted stuff
   */
  def apply(url: String, md5: String): File = {


    require((cache.exists() && cache.isDirectory) || cache.mkdirs())

    val name = url.split('/').last

    val cached = new File(cache, name)

    // check whatever has been downloaded already
    if (null != md5 && cached.exists()) {
      // TODO ; check the MD5 and delete
    }

    // download whatever if it's not there
    if (!cached.exists()) {
      copy(
        new URL(url).openStream(),
        new FileOutputStream(cached)
      ).close()
    }

    // TODO ; require the MD5 matches

    // try to uncompress a zip
    try {
      val zip = new ZipFile(cached)
      val root = new File(dump, name)

      val subfix = cached.getName.substring(0, cached.getName.lastIndexOf('.')) + "/"
      var subfixValid = true

      // - walk files and dump them
      val entries: util.Enumeration[_ <: ZipEntry] = zip.entries()
      while (entries.hasMoreElements) {
        val entry: ZipEntry = entries.nextElement()

        if (!entry.isDirectory)
          copy(
            zip.getInputStream(entry),
            new FileOutputStream({
              val entryName: String = entry.getName

              subfixValid = subfixValid && entryName.startsWith(subfix)
              if (false)
                require(
                  entryName.startsWith(subfix),
                  "Uh oh - `%s` does not start with `%s".format(entryName, subfix)
                )

              val dumped = new File(root, entryName)

              require(
                (dumped.getParentFile.exists() && dumped.getParentFile.isDirectory) || dumped.getParentFile.mkdirs(),
                "Problem creating parent file of `%s`".format(entryName)
              )

              dumped
            })).close()
      }

      if (subfixValid) new File(root, subfix) else root
    } catch {
      case e: ZipException =>
        sys.error(e.getClass.getName + '(' + e.getMessage + ')')

        // TODO ; check if it's a TAR.GZ
        ???

        // TODO ; - walk files and dump them
        ???

        // TODO ; fail - we can't read this file
        ???
    }
  }

  def apply(url: String): File = apply(url, null)
}
