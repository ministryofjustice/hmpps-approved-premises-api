package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ConditionTypes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.ElectronicMonitoringAdditionalConditionWithRestriction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.GenericAdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.Licence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.LicenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.licence.MultipleExclusionZoneAdditionalCondition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdditionalConditionFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApConditionsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LicenceConditionsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LicenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LicenceSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PssConditionsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.licenceApiMockNotFoundLicenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.licenceApiMockNotFoundLicenceSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.licenceApiMockSuccessfulLicenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.licenceApiMockSuccessfulLicenceSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject

class PersonLicenceTest : IntegrationTestBase() {

  private fun aLicenceSummary(id: Long, crn: String, status: LicenceStatus): LicenceSummary = LicenceSummaryFactory()
    .withId(id)
    .withCrn(crn)
    .withStatus(status)
    .withLicenceType(LicenceType.AP)
    .produce()

  private fun aLicence(id: Long, crn: String): Licence = LicenceFactory()
    .withId(id)
    .withCrn(crn)
    .withStatus(LicenceStatus.ACTIVE)
    .withLicenceType(LicenceType.AP)
    .produce()

  @Test
  fun `returns 403 when user does not have CAS1_AP_RESIDENT_PROFILE permission`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `returns 404 when licence summaries endpoint returns 404`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    licenceApiMockNotFoundLicenceSummaries(crn)

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `returns 403 when licence summaries endpoint returns 403`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    mockUnsuccessfulGetCall(url = "/public/licence-summaries/crn/$crn", responseStatus = 403)

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `returns 404 when no ACTIVE licence summary exists`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    val summaries = listOf(
      aLicenceSummary(1, crn, LicenceStatus.APPROVED),
      aLicenceSummary(2, crn, LicenceStatus.VARIATION_IN_PROGRESS),
    )
    licenceApiMockSuccessfulLicenceSummaries(crn, summaries)

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `returns 404 when details endpoint returns 404 for ACTIVE summary`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    val activeId = 99L
    val summaries = listOf(aLicenceSummary(activeId, crn, LicenceStatus.ACTIVE))
    licenceApiMockSuccessfulLicenceSummaries(crn, summaries)
    licenceApiMockNotFoundLicenceDetails(activeId)

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `returns 403 when details endpoint returns 403 for ACTIVE summary`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    val activeId = 100L
    val summaries = listOf(aLicenceSummary(activeId, crn, LicenceStatus.ACTIVE))
    licenceApiMockSuccessfulLicenceSummaries(crn, summaries)
    mockUnsuccessfulGetCall(url = "/public/licences/id/$activeId", responseStatus = 403)

    webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `returns 200 with licence body when ACTIVE summary exists and details found`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    val activeId = 101L
    val summaries = listOf(aLicenceSummary(activeId, crn, LicenceStatus.ACTIVE))
    val licence = aLicence(activeId, crn)

    licenceApiMockSuccessfulLicenceSummaries(crn, summaries)
    licenceApiMockSuccessfulLicenceDetails(licence)

    val response: Licence = webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject()

    assertThat(response).isEqualTo(licence)
  }

  @Test
  fun `returns 200 with licence body containing additional conditions`() {
    val crn = "X12345"
    val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))

    val activeId = 102L
    val summaries = listOf(aLicenceSummary(activeId, crn, LicenceStatus.ACTIVE))

    val additionalConditions = listOf(
      AdditionalConditionFactory().withType(ConditionTypes.STANDARD).produce(),
      AdditionalConditionFactory().withType(ConditionTypes.ELECTRONIC_MONITORING).produce(),
      AdditionalConditionFactory().withType(ConditionTypes.MULTIPLE_EXCLUSION_ZONE).produce(),
    )

    val licence = LicenceFactory()
      .withId(activeId)
      .withStatus(LicenceStatus.ACTIVE)
      .withCrn(crn)
      .withConditions(
        LicenceConditionsFactory()
          .withApConditions(
            ApConditionsFactory()
              .withAdditional(additionalConditions)
              .produce(),
          )
          .withPssConditions(
            PssConditionsFactory()
              .withAdditional(additionalConditions)
              .produce(),
          )
          .produce(),
      )
      .produce()

    licenceApiMockSuccessfulLicenceSummaries(crn, summaries)
    licenceApiMockSuccessfulLicenceDetails(licence)

    val response: Licence = webTestClient.get()
      .uri("/cas1/people/$crn/licence-details")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject()

    assertThat(response.conditions.apConditions.additional[0]).isInstanceOf(GenericAdditionalCondition::class.java)
    assertThat(response.conditions.apConditions.additional[1]).isInstanceOf(ElectronicMonitoringAdditionalConditionWithRestriction::class.java)
    assertThat(response.conditions.apConditions.additional[2]).isInstanceOf(MultipleExclusionZoneAdditionalCondition::class.java)

    assertThat(response.conditions.pssConditions.additional[0]).isInstanceOf(GenericAdditionalCondition::class.java)
    assertThat(response.conditions.pssConditions.additional[1]).isInstanceOf(ElectronicMonitoringAdditionalConditionWithRestriction::class.java)
    assertThat(response.conditions.pssConditions.additional[2]).isInstanceOf(MultipleExclusionZoneAdditionalCondition::class.java)

    assertThat(response).isEqualTo(licence)
  }
}
