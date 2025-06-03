package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockUserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextUserAccessEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OASysNeedsQuestionTransformer

class Cas1OAsysTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var cas1OASysNeedsQuestionTransformer: Cas1OASysNeedsQuestionTransformer

  @Nested
  inner class OptionalNeeds {

    @Test
    fun `No JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/oasys/optional-needs-questions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Return 404 if access record can't be found for the CRN`() {
      val (_, jwt) = givenAUser()
      val crn = "CRN123"

      apDeliusContextUserAccessEmptyResponse()

      webTestClient.get()
        .uri("/cas1/people/$crn/oasys/optional-needs-questions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Return forbidden if user can't access the CRN`() {
      val (_, jwt) = givenAUser()
      val crn = "CRN123"

      apDeliusContextMockUserAccess(
        CaseAccessFactory()
          .withCrn(crn)
          .withUserExcluded(true)
          .produce(),
      )

      webTestClient.get()
        .uri("/cas1/people/$crn/oasys/optional-needs-questions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser()
      val (offenderDetails) = givenAnOffender()

      val needsDetails = NeedsDetailsFactory().apply {
        withAssessmentId(34853487)
        withAccommodationIssuesDetails("Accommodation", true, false)
        withAttitudeIssuesDetails("Attitude", false, true)
        withFinanceIssuesDetails(null, null, null)
      }.produce()

      apOASysContextMockSuccessfulNeedsDetailsCall(offenderDetails.otherIds.crn, needsDetails)

      webTestClient.get()
        .uri("/cas1/people/${offenderDetails.otherIds.crn}/oasys/optional-needs-questions")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            cas1OASysNeedsQuestionTransformer.transformToNeedsQuestion(needsDetails),
          ),
        )
    }
  }
}
