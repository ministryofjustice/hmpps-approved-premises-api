package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatedClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.time.LocalDate
import java.time.OffsetDateTime

class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var inboundMessageListener: InboundMessageListener

  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  @BeforeEach
  fun clearMessages() {
    inboundMessageListener.clearMessages()
  }

  @Test
  fun `Get all assessments without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource
  @NullSource
  fun `Get all assessments returns 200 with correct body`(assessmentDecision: AssessmentDecision?) {
    `Given a User` { user, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
          withDecision(assessmentDecision)
        }

        assessment.schemaUpToDate = true

        val reallocatedAssessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(user)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
          withReallocatedAt(OffsetDateTime.now())
        }

        reallocatedAssessment.schemaUpToDate = true

        webTestClient.get()
          .uri("/assessments")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf(assessmentTransformer.transformDomainToApiSummary(toAssessmentSummaryEntity(assessment), offenderDetails, inmateDetails))
            )
          )
      }
    }
  }

  private fun toAssessmentSummaryEntity(assessment: AssessmentEntity): DomainAssessmentSummary =
    DomainAssessmentSummary(
      type = when (assessment.application) {
        is ApprovedPremisesApplicationEntity -> "approved-premises"
        is TemporaryAccommodationApplicationEntity -> "temporary-accommodation"
        else -> fail()
      },

      id = assessment.id,

      applicationId = assessment.application.id,

      createdAt = assessment.createdAt,

      riskRatings = when (val reified = assessment.application) {
        is ApprovedPremisesApplicationEntity -> reified.riskRatings?.let { objectMapper.writeValueAsString(it) }
        else -> null
      },

      arrivalDate = when (val application = assessment.application) {
        is ApprovedPremisesApplicationEntity -> application.arrivalDate
        else -> null
      },

      dateOfInfoRequest = assessment
        .clarificationNotes
        .filter { it.response == null }
        .minByOrNull { it.createdAt }
        ?.createdAt,

      completed = assessment.decision != null,
      decision = assessment.decision?.name,
      crn = assessment.application.crn,
      isStarted = assessment.data != null
    )

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get assessment by ID returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        webTestClient.get()
          .uri("/assessments/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
            )
          )
      }
    }
  }

  @Test
  fun `Accept assessment without JWT returns 401`() {
    val placementRequirements = PlacementRequirements(
      gender = Gender.male,
      type = ApType.normal,
      expectedArrival = LocalDate.now(),
      duration = 12,
      location = "B74",
      radius = 50,
      essentialCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.hasEnSuite),
      desirableCriteria = listOf(PlacementCriteria.isCatered, PlacementCriteria.acceptsSexOffenders),
      notes = "Some Notes",
    )

    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
      .bodyValue(AssessmentAcceptance(document = "{}", requirements = placementRequirements))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Accept assessment returns 200, persists decision, creates and allocates a placement request, and emits SNS domain event message when requirements provided`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher2, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withApplicationSchema(applicationSchema)
            }

            val assessment = assessmentEntityFactory.produceAndPersist {
              withAllocatedToUser(userEntity)
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
            }

            val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

            assessment.schemaUpToDate = true

            val essentialCriteria = listOf(PlacementCriteria.hasEnSuite, PlacementCriteria.isRecoveryFocussed)
            val desirableCriteria = listOf(PlacementCriteria.acceptsNonSexualChildOffenders, PlacementCriteria.acceptsSexOffenders)

            val placementRequirements = PlacementRequirements(
              gender = Gender.male,
              type = ApType.normal,
              expectedArrival = LocalDate.now(),
              duration = 12,
              location = postcodeDistrict.outcode,
              radius = 50,
              essentialCriteria = essentialCriteria,
              desirableCriteria = desirableCriteria,
              notes = "Some Notes",
            )

            webTestClient.post()
              .uri("/assessments/${assessment.id}/acceptance")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequirements))
              .exchange()
              .expectStatus()
              .isOk

            val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
            assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
            assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
            assertThat(persistedAssessment.submittedAt).isNotNull

            val emittedMessage = inboundMessageListener.blockForMessage()

            assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.assessed")
            assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
            assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
            assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
            assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
              SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
              SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
            )

            val persistedPlacementRequest = placementRequestRepository.findByApplication(application)!!

            assertThat(persistedPlacementRequest.allocatedToUser.id).isIn(listOf(matcher1.id, matcher2.id))
            assertThat(persistedPlacementRequest.application.id).isEqualTo(application.id)
            assertThat(persistedPlacementRequest.duration).isEqualTo(placementRequirements.duration)
            assertThat(persistedPlacementRequest.apType).isEqualTo(placementRequirements.type)
            assertThat(persistedPlacementRequest.expectedArrival).isEqualTo(placementRequirements.expectedArrival)
            assertThat(persistedPlacementRequest.gender).isEqualTo(placementRequirements.gender)
            assertThat(persistedPlacementRequest.postcodeDistrict.outcode).isEqualTo(placementRequirements.location)
            assertThat(persistedPlacementRequest.radius).isEqualTo(placementRequirements.radius)
            assertThat(persistedPlacementRequest.notes).isEqualTo(placementRequirements.notes)

            assertThat(persistedPlacementRequest.desirableCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(placementRequirements.desirableCriteria.map { it.toString() })
            assertThat(persistedPlacementRequest.essentialCriteria.map { it.propertyName }).containsExactlyInAnyOrderElementsOf(placementRequirements.essentialCriteria.map { it.toString() })
          }
        }
      }
    }
  }

  @Test
  fun `Accept assessment returns 200, persists decision, does not create a Placement Request, and emits SNS domain event message when requirements not provided`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher2, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withApplicationSchema(applicationSchema)
            }

            val assessment = assessmentEntityFactory.produceAndPersist {
              withAllocatedToUser(userEntity)
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
            }

            val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

            assessment.schemaUpToDate = true

            webTestClient.post()
              .uri("/assessments/${assessment.id}/acceptance")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = null))
              .exchange()
              .expectStatus()
              .isOk

            val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
            assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
            assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
            assertThat(persistedAssessment.submittedAt).isNotNull

            val emittedMessage = inboundMessageListener.blockForMessage()

            assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.assessed")
            assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
            assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
            assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
            assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
              SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
              SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
            )

            assertThat(placementRequestRepository.findByApplication(application)).isNull()
          }
        }
      }
    }
  }

  @Test
  fun `Accept assessment returns an error if the postcode cannot be found`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher1, _ ->
        `Given a User`(roles = listOf(UserRole.MATCHER)) { matcher2, _ ->
          `Given an Offender` { offenderDetails, inmateDetails ->
            val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            }

            val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
              withAddedAt(OffsetDateTime.now())
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withApplicationSchema(applicationSchema)
            }

            val assessment = assessmentEntityFactory.produceAndPersist {
              withAllocatedToUser(userEntity)
              withApplication(application)
              withAssessmentSchema(assessmentSchema)
            }

            assessment.schemaUpToDate = true

            val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isEsap)
            val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

            val placementRequirements = PlacementRequirements(
              gender = Gender.male,
              type = ApType.normal,
              expectedArrival = LocalDate.now(),
              duration = 12,
              location = "SW1",
              radius = 50,
              essentialCriteria = essentialCriteria,
              desirableCriteria = desirableCriteria,
            )

            webTestClient.post()
              .uri("/assessments/${assessment.id}/acceptance")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequirements))
              .exchange()
              .expectStatus()
              .is4xxClientError
              .expectBody()
              .jsonPath("title").isEqualTo("Bad Request")
              .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
              .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.postcodeDistrict")
          }
        }
      }
    }
  }

  @Test
  fun `Reject assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/rejection")
      .bodyValue(AssessmentRejection(document = "{}", rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reject assessment returns 200, persists decision, emits SNS domain event message`() {
    `Given a User`(
      staffUserDetailsConfigBlock = { withProbationAreaCode("N21") }
    ) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        assessment.schemaUpToDate = true

        webTestClient.post()
          .uri("/assessments/${assessment.id}/rejection")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(AssessmentRejection(document = mapOf("document" to "value"), rejectionRationale = "reasoning"))
          .exchange()
          .expectStatus()
          .isOk

        val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
        assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
        assertThat(persistedAssessment.submittedAt).isNotNull

        val emittedMessage = inboundMessageListener.blockForMessage()

        assertThat(emittedMessage.eventType).isEqualTo("approved-premises.application.assessed")
        assertThat(emittedMessage.description).isEqualTo("An application has been assessed for an Approved Premises placement")
        assertThat(emittedMessage.detailUrl).matches("http://frontend/events/application-assessed/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
        assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(assessment.application.id)
        assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
          SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
          SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!)
        )
      }
    }
  }

  @Test
  fun `Create clarification note returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        webTestClient.post()
          .uri("/assessments/${assessment.id}/notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewClarificationNote(
              query = "some text"
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.query").isEqualTo("some text")
      }
    }
  }

  @Test
  fun `Update clarification note returns 201 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
        }

        val assessment = assessmentEntityFactory.produceAndPersist {
          withAllocatedToUser(userEntity)
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessment)
          withCreatedBy(userEntity)
        }

        webTestClient.put()
          .uri("/assessments/${assessment.id}/notes/${clarificationNote.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatedClarificationNote(
              response = "some text",
              responseReceivedOn = LocalDate.parse("2022-03-04")
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.response").isEqualTo("some text")
          .jsonPath("$.responseReceivedOn").isEqualTo("2022-03-04")
      }
    }
  }
}
