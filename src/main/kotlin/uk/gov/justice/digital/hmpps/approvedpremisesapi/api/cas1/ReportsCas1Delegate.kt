package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import java.util.Optional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ReportName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

/**
 * A delegate to be called by the {@link ReportsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")
interface ReportsCas1Delegate {

  fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

  /**
   * @see ReportsCas1#getReportByName
   */
  fun getReportByName(
    xServiceName: ServiceName,
    reportName: Cas1ReportName,
    year: kotlin.Int?,
    month: kotlin.Int?,
    startDate: java.time.LocalDate?,
    endDate: java.time.LocalDate?,
  ): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> = ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
}
