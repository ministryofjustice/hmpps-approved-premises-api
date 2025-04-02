package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ApplicationTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class GetApplications {
    @ParameterizedTest
    @CsvSource("/applications", "/cas3/applications")
    fun `Get all applications without JWT returns 401`(baseUrl: String) {
      webTestClient.get()
        .uri(baseUrl)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @CsvSource(
      "CAS3_REFERRER,/applications",
      "CAS3_ASSESSOR,/applications",
      "CAS3_REFERRER,/cas3/applications",
      "CAS3_ASSESSOR,/cas3/applications",
    )
    fun `Get all applications returns 200 for TA - returns all applications for user`(userRole: UserRole, baseUrl: String) {
      givenAProbationRegion { probationRegion ->
        givenAUser(roles = listOf(userRole), probationRegion = probationRegion) { otherUser, _ ->
          givenAUser(
            roles = listOf(UserRole.CAS3_REFERRER),
            probationRegion = probationRegion,
          ) { referrerUser, jwt ->
            givenAnOffender { offenderDetails, _ ->
              temporaryAccommodationApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val application =
                createApplicationEntity(applicationSchema, referrerUser, offenderDetails, probationRegion, null)

              val anotherUsersApplication =
                createApplicationEntity(applicationSchema, otherUser, offenderDetails, probationRegion, null)

              apDeliusContextAddResponseToUserAccessCall(
                listOf(
                  CaseAccessFactory()
                    .withCrn(offenderDetails.otherIds.crn)
                    .produce(),
                ),
                referrerUser.deliusUsername,
              )

              when (baseUrl) {
                "/applications" -> {
                  val responseBody = webTestClient.get()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer $jwt")
                    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .bodyAsListOfObjects<TemporaryAccommodationApplicationSummary>()

                  assertThat(responseBody).anyMatch {
                    application.id == it.id &&
                      application.crn == it.person.crn &&
                      application.createdAt.toInstant() == it.createdAt &&
                      application.createdByUser.id == it.createdByUserId &&
                      application.submittedAt?.toInstant() == it.submittedAt
                  }

                  assertThat(responseBody).noneMatch {
                    anotherUsersApplication.id == it.id
                  }
                }

                else -> {
                  val responseBody = webTestClient.get()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer $jwt")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .bodyAsListOfObjects<Cas3ApplicationSummary>()

                  assertThat(responseBody).anyMatch {
                    application.id == it.id &&
                      application.crn == it.person.crn &&
                      application.createdAt.toInstant() == it.createdAt &&
                      application.createdByUser.id == it.createdByUserId &&
                      application.submittedAt?.toInstant() == it.submittedAt
                  }

                  assertThat(responseBody).noneMatch {
                    anotherUsersApplication.id == it.id
                  }
                }
              }
            }
          }
        }
      }
    }

    private fun createApplicationEntity(
      applicationSchema: TemporaryAccommodationApplicationJsonSchemaEntity,
      user: UserEntity,
      offenderDetails: OffenderDetailSummary,
      probationRegion: ProbationRegionEntity,
      submittedAt: OffsetDateTime?,
    ): TemporaryAccommodationApplicationEntity = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
      withSubmittedAt(submittedAt)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
      withProbationRegion(probationRegion)
    }
  }

  @Nested
  inner class SoftDelete {
    @Test
    fun `soft delete application without JWT returns 401`() {
      webTestClient.delete()
        .uri("/cas3/applications/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `soft delete inProgress application successfully returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          val application =
            persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)

          webTestClient.delete()
            .uri("/cas3/applications/${application.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk

          Assertions.assertThat(temporaryAccommodationApplicationRepository.findById(application.id).get().deletedAt)
            .isNotNull()

          val domainEvents =
            domainEventRepository.findByApplicationIdAndType(
              applicationId = application.id,
              type = DomainEventType.CAS3_DRAFT_REFERRAL_DELETED,
            )

          assertThat(domainEvents.size).isEqualTo(1)
        }
      }
    }
  }

  private fun persistApplication(crn: String, user: UserEntity) = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withApplicationSchema(persistApplicationSchema())
    withProbationRegion(user.probationRegion)
    withArrivalDate(LocalDate.now().plusDays(30))
    withSubmittedAt(null)
  }

  private fun persistApplicationSchema() = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
    temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
  }
}
