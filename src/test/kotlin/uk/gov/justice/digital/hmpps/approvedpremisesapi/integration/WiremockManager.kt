package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.WireMockServer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.ServerSocket
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object WiremockPortManager {

  private var channel: FileChannel? = null

  private val log = LoggerFactory.getLogger(this::class.java)

  fun reserveFreePort(): Int {
    synchronized(this) {
      var port: Int? = null
      var attempts = 0
      while (port == null) {
        if (attempts > 100) {
          error("After 100 attempts, i can't find a free port for wiremock")
        }

        val portToTry = ServerSocket(0).localPort

        log.info("Trying Wiremock port: $portToTry")
        val lockFilePath = Paths.get("${System.getProperty("java.io.tmpdir")}${System.getProperty("file.separator")}ap-int-port-lock-$portToTry.lock")

        try {
          channel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
          channel!!.position(0)

          if (channel!!.tryLock() == null) {
            log.info("Port $portToTry is in use")
            channel!!.close()
            channel = null
            attempts += 1
            continue
          }

          log.info("Using Wiremock port: $portToTry")
          port = portToTry
        } catch (_: Exception) {
        }
      }

      return port
    }
  }

  fun releasePort() = channel?.close()
}

@Component
class WiremockManager {

  private val log = LoggerFactory.getLogger(this::class.java)

  lateinit var wiremockServer: WireMockServer

  @Value("\${wiremock.port}")
  lateinit var wiremockPort: Number

  fun beforeTest() {
    if (!this::wiremockServer.isInitialized || !wiremockServer.isRunning) {
      log.info("Starting wiremock on port $wiremockPort")
      wiremockServer = WireMockServer(wiremockPort.toInt())
      wiremockServer.start()
    }
  }

  fun afterTest() {
    wiremockServer.resetAll()
  }

  fun resetRequestJournal() {
    wiremockServer.resetRequests()
  }
}
