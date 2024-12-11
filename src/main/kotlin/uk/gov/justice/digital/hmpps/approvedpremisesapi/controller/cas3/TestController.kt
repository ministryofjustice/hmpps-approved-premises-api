package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas3

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AllocatedFilter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CacheType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ReportType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField

@RestController
@RequestMapping(path = ["/cas3"])
class TestController {

  @GetMapping(path = ["/test/{testEnum}"], produces = ["application/json"])
  fun test(
    @RequestBody(required = true)
    request: ModelsToInclude,
  ): String {
    return "Please work"
  }
}

data class ModelsToInclude(
  val allocatedFilter: AllocatedFilter,
  val anyValue: AnyValue,
  val applicationSortField: ApplicationSortField,
  val assessmentSortField: AssessmentSortField,
  val bookingBody: BookingBody,
  val bookingSearchSortField: BookingSearchSortField,
  val cachetType: CacheType,
  val cas3ReportType: Cas3ReportType,
  val offlineApplicationSummary: OfflineApplicationSummary,
  val placementRequestSortField: PlacementRequestSortField,
  val testEEnum: TestEEnum,
)

data class AnyValue(
  val anyValue: Map<String, Any>,
)

enum class TestEEnum {
  A,
  B,
  C,
}
