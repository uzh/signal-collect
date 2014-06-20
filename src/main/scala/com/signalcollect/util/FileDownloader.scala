package com.signalcollect.util

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.zip.GZIPInputStream

object FileDownloader {
 
def downloadFile(url: URL, localFileName: String) {
    val in = Channels.newChannel(url.openStream)
    val out = new FileOutputStream(localFileName)
    out.getChannel.transferFrom(in, 0, Int.MaxValue)
    in.close
    out.close
  }

  def decompressGzip(archive: String, decompressedName: String) {
    val zin = new GZIPInputStream(new FileInputStream(archive))
    val os = new FileOutputStream(decompressedName)
    val buffer = new Array[Byte](2048)
    var read = zin.read(buffer)
    while (read > 0) {
      os.write(buffer, 0, read)
      read = zin.read(buffer)
    }
    os.close
    zin.close
  }
  
}