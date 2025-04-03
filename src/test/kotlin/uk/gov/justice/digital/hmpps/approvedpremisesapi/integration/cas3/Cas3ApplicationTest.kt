package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
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

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class Cas3CreateApplication {

    @Test
    fun `Create new application returns 403 when user isn't  CAS3_REFERRER role`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Should get 403 forbidden error when create new application with user CAS3_REPORTER`() {
      givenAUser(roles = listOf(UserRole.CAS3_REPORTER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          callCasApi(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden

          callCas3Api(jwt, offenderDetails.otherIds.crn, "789")
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and Location header`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id)
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and store prison-name in DB`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt ->
        val agencyName = "HMP Bristol"
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withCrn("CRN")
            withDateOfBirth(LocalDate.parse("1985-05-05"))
            withNomsNumber("NOMS321")
            withFirstName("James")
            withLastName("Someone")
            withGender("Male")
            withEthnicity("White British")
            withNationality("English")
            withReligionOrBelief("Judaism")
            withGenderIdentity("Prefer to self-describe")
            withSelfDescribedGenderIdentity("This is a self described identity")
          },
          inmateDetailsConfigBlock = {
            withOffenderNo("NOMS321")
            withCustodyStatus(InmateStatus.IN)
            withAssignedLivingUnit(
              AssignedLivingUnit(
                agencyId = "BRI",
                locationId = 5,
                description = "B-2F-004",
                agencyName = agencyName,
              ),
            )
          },
        ) { offenderDetails, _ ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          callCasApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id, agencyName)
          callCas3ApiAndAssertResponse(jwt, offenderDetails.otherIds.crn, applicationSchema.id, agencyName)
        }
      }
    }

    private fun callCasApiAndAssertResponse(
      jwt: String,
      crn: String,
      applicationSchemaId: UUID,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val apiResponse = callCasApi(jwt, crn, offenceId)
      val result =
        apiResponse
          .expectStatus()
          .isCreated
          .returnResult(TemporaryAccommodationApplication::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.schemaVersion == applicationSchemaId &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCasApi(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(
        NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()

    private fun callCas3ApiAndAssertResponse(
      jwt: String,
      crn: String,
      applicationSchemaId: UUID,
      agencyName: String? = null,
    ) {
      val offenceId = "789"

      val apiResponse = callCas3Api(jwt, crn, offenceId)
      val result =
        apiResponse
          .expectStatus()
          .isCreated
          .returnResult(Cas3Application::class.java)

      assertThat(result.responseHeaders["Location"]).anyMatch {
        it.matches(Regex("/applications/.+"))
      }

      val blockFirst = result.responseBody.blockFirst()
      assertThat(blockFirst).matches {
        it.person.crn == crn &&
          it.schemaVersion == applicationSchemaId &&
          it.offenceId == offenceId
      }

      if (agencyName != null) {
        val accommodationApplicationEntity =
          temporaryAccommodationApplicationRepository.findByIdOrNull(blockFirst.id)
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isNotNull()
        assertThat(accommodationApplicationEntity!!.prisonNameOnCreation).isEqualTo(agencyName)
      }
    }

    private fun callCas3Api(jwt: String, crn: String, offenceId: String) = webTestClient.post()
      .uri("/cas3/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Cas3NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = offenceId,
        ),
      )
      .exchange()
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
