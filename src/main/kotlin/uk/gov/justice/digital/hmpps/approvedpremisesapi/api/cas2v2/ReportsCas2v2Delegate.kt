package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ReportName
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ReportsCas2v2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.13.0")interface ReportsCas2v2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ReportsCas2v2#reportsReportNameGet
     */
    fun reportsReportNameGet(reportName: Cas2v2ReportName): ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> {
                        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
                    }

}
