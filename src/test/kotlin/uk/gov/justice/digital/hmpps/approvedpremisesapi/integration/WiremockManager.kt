package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.WireMockServer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object WiremockPortHolder {
  private val possiblePorts = (57830..57880).shuffled()

  private var port: Int? = null
  private var channel: FileChannel? = null

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPort(): Int {
    synchronized(this) {
      if (port != null) {
        return port!!
      }

      possiblePorts.forEach { portToTry ->
        log.info("Trying Wiremock port: $portToTry")
        val lockFilePath = Paths.get("${System.getProperty("java.io.tmpdir")}${System.getProperty("file.separator")}ap-int-port-lock-$portToTry.lock")

        try {
          channel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
          channel!!.position(0)

          if (channel!!.tryLock() == null) {
            log.info("Port $portToTry is in use")
            channel!!.close()
            channel = null
            return@forEach
          }

          log.info("Using Wiremock port: $portToTry")
          port = portToTry

          return portToTry
        } catch (_: Exception) {
        }
      }

      error("Could not lock any potential Wiremock ports")
    }
  }

  fun releasePort() = channel?.close()
}

@Component
class WiremockManager {

  lateinit var wiremockServer: WireMockServer

  @Value("\${wiremock.port}")
  lateinit var wiremockPort: Number

  fun setupTests() {
    wiremockServer = WireMockServer(wiremockPort.toInt())
    wiremockServer.start()
  }

  fun teardownTests() {
    wiremockServer.stop()
  }
}
