package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.util.UriUtils
import java.nio.charset.Charset

interface TelemetryService {
  fun trackEvent(event: Event)

  data class Event(
    val name: String,
    val properties: Map<String, String?>,
    val metrics: Map<String, Double?>,
  )
}

@Service
class TelemetryServiceImpl(
  private val telemetryClient: TelemetryClient = TelemetryClient(),
) : TelemetryService {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async
  override fun trackEvent(event: TelemetryService.Event) {
    log.debug(
      "{} {} {}",
      UriUtils.encode(event.name, Charset.defaultCharset()),
      UriUtils.encode(event.properties.toString(), Charset.defaultCharset()),
      UriUtils.encode(event.metrics.toString(), Charset.defaultCharset()),
    )
    telemetryClient.trackEvent(
      event.name,
      event.properties.filterValues { it != null },
      event.metrics.filterValues { it != null },
    )
  }
}
