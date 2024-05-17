package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer

class PersonalTimelineTest : IntegrationTestBase() {
  @Autowired
  lateinit var personTransformer: PersonTransformer

  @Test
  fun `Getting a personal timeline for a CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/timeline")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a personal timeline for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "other-auth-source",
    )

    webTestClient.get()
      .uri("/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN with a NOMIS JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN with ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_POM"),
    )

    webTestClient.get()
      .uri("/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN that does not exist returns 404`() {
    `Given a User` { _, jwt ->
      wiremockServer.stubFor(
        get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(404),
          ),
      )

      webTestClient.get()
        .uri("/people/CRN/timeline")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        `Given an Application`(userEntity, crn = offenderDetails.otherIds.crn) { application ->
          val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
            withCrn(offenderDetails.otherIds.crn)
            withApplicationId(application.id)
          }

          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = inmateDetails,
          )

          webTestClient.get()
            .uri("/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = listOf(
                    ApplicationTimeline(
                      id = application.id,
                      createdAt = application.createdAt.toInstant(),
                      status = ApprovedPremisesApplicationStatus.started,
                      isOfflineApplication = false,
                      createdBy = ApprovedPremisesUser(
                        qualifications = emptyList(),
                        roles = emptyList(),
                        apArea = ApArea(
                          id = userEntity.apArea!!.id,
                          identifier = userEntity.apArea!!.identifier,
                          name = userEntity.apArea!!.name,
                        ),
                        service = "CAS1",
                        id = userEntity.id,
                        name = userEntity.name,
                        deliusUsername = userEntity.deliusUsername,
                        region = ProbationRegion(
                          id = userEntity.probationRegion.id,
                          name = userEntity.probationRegion.name,
                        ),
                        email = userEntity.email,
                        telephoneNumber = userEntity.telephoneNumber,
                        isActive = userEntity.isActive,
                      ),
                      timelineEvents = listOf(
                        TimelineEvent(
                          type = TimelineEventType.approvedPremisesApplicationSubmitted,
                          id = domainEvents[0].id.toString(),
                          occurredAt = domainEvents[0].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            TimelineEventAssociatedUrl(
                              type = TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                        TimelineEvent(
                          type = TimelineEventType.approvedPremisesApplicationSubmitted,
                          id = domainEvents[1].id.toString(),
                          occurredAt = domainEvents[1].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            TimelineEventAssociatedUrl(
                              type = TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN with no applications returns OK with correct body`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val personInfoResult = PersonInfoResult.Success.Full(
          crn = offenderDetails.otherIds.crn,
          offenderDetailSummary = offenderDetails,
          inmateDetail = inmateDetails,
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/timeline")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              PersonalTimeline(
                person = personTransformer.transformModelToPersonApi(personInfoResult),
                applications = emptyList(),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN without a NomsNumber returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->
        `Given an Application`(userEntity, crn = offenderDetails.otherIds.crn) { application ->
          val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
            withCrn(offenderDetails.otherIds.crn)
            withApplicationId(application.id)
          }

          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = null,
          )

          webTestClient.get()
            .uri("/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = listOf(
                    ApplicationTimeline(
                      id = application.id,
                      createdAt = application.createdAt.toInstant(),
                      status = ApprovedPremisesApplicationStatus.started,
                      isOfflineApplication = false,
                      createdBy = ApprovedPremisesUser(
                        qualifications = emptyList(),
                        roles = emptyList(),
                        apArea = ApArea(
                          id = userEntity.apArea!!.id,
                          identifier = userEntity.apArea!!.identifier,
                          name = userEntity.apArea!!.name,
                        ),
                        service = "CAS1",
                        id = userEntity.id,
                        name = userEntity.name,
                        deliusUsername = userEntity.deliusUsername,
                        region = ProbationRegion(
                          id = userEntity.probationRegion.id,
                          name = userEntity.probationRegion.name,
                        ),
                        email = userEntity.email,
                        telephoneNumber = userEntity.telephoneNumber,
                        isActive = userEntity.isActive,
                      ),
                      timelineEvents = listOf(
                        TimelineEvent(
                          type = TimelineEventType.approvedPremisesApplicationSubmitted,
                          id = domainEvents[0].id.toString(),
                          occurredAt = domainEvents[0].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            TimelineEventAssociatedUrl(
                              type = TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                        TimelineEvent(
                          type = TimelineEventType.approvedPremisesApplicationSubmitted,
                          id = domainEvents[1].id.toString(),
                          occurredAt = domainEvents[1].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            TimelineEventAssociatedUrl(
                              type = TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }
  }
}
