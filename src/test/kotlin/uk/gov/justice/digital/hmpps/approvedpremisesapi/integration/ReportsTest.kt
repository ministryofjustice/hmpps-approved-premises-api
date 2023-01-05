package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties

class ReportsTest : IntegrationTestBase() {
  @Test
  fun `Get bookings report returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val keyWorker = ContextStaffMemberFactory().produce()
    mockStaffMembersContextApiCall(keyWorker, premises.qCode)

    val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
      withPremises(premises)
      withServiceName(ServiceName.approvedPremises)
      withStaffKeyWorkerCode(keyWorker.code)
      withCrn("CRN123")
    }

    bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
    bookings[2].let {
      it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
      it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
      it.departure = departureEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
        withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
        withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
      }
    }
    bookings[3].let {
      it.cancellation = cancellationEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
      }
    }
    bookings[4].let {
      it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
      }
    }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val expectedDataFrame = BookingsReportGenerator()
      .createReport(bookings, BookingsReportProperties(ServiceName.approvedPremises, null))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/reports/bookings")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .consumeWith {
        val actual = DataFrame
          .readExcel(it.responseBody!!.inputStream())
          .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
        assertThat(actual).isEqualTo(expectedDataFrame)
      }
  }
}
