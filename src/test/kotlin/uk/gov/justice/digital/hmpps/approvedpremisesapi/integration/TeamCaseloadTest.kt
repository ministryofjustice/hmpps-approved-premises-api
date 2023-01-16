package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CaseLoadPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ManagedOffenderFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.TeamCaseLoad

class TeamCaseloadTest : IntegrationTestBase() {
  @Test
  fun `Attempting to get Team Caseload without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/team-caseload")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Attempting to get Team Caseload with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis"
    )

    webTestClient.get()
      .uri("/people/team-caseload")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting Team Caseload returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_PROBATION")
    )

    val team1Offender = ManagedOffenderFactory()
      .withOffenderCrn("CRN1")
      .produce()

    val team2Offender = ManagedOffenderFactory()
      .withOffenderCrn("CRN2")
      .produce()

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername("username")
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce(),
            StaffUserTeamMembershipFactory()
              .withCode("TEAM2")
              .produce()
          )
        )
        .produce()
    )

    mockTeamCaseloadCall(
      "TEAM1",
      TeamCaseLoad(
        managedOffenders = listOf(team1Offender)
      )
    )

    mockTeamCaseloadCall(
      "TEAM2",
      TeamCaseLoad(
        managedOffenders = listOf(team2Offender)
      )
    )

    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn("CRN1")
        .withFirstName("Some")
        .withLastName("Person")
        .produce()
    )

    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn("CRN2")
        .withFirstName("Another")
        .withLastName("Person")
        .produce()
    )

    webTestClient.get()
      .uri("/people/team-caseload")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          listOf(
            CaseLoadPerson(
              crn = "CRN1",
              firstName = "Some",
              surname = "Person"
            ),
            CaseLoadPerson(
              crn = "CRN2",
              firstName = "Another",
              surname = "Person"
            )
          )
        )
      )
  }
}
