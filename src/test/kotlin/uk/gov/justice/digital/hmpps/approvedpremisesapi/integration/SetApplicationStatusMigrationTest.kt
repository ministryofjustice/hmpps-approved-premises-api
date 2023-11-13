package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.OffsetDateTime

class SetApplicationStatusMigrationTest : MigrationJobTestBase() {
  lateinit var taSchema: TemporaryAccommodationApplicationJsonSchemaEntity
  lateinit var schema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var user: UserEntity

  @BeforeEach
  fun setup() {
    taSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    schema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
    user = userEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }
    temporaryAccommodationApplicationEntityFactory.produceAndPersistMultiple(5) {
      withApplicationSchema(taSchema)
      withCreatedByUser(user)
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }
  }

  @Test
  fun `it should set the correct status on all applications`() {
    val (applicationWithNoAssessment, _) = createApplication()

    val (applicationWithAssessmentWithNoData, _) = createApplication(
      assessmentConfigBlock = {
        withData(null)
        withSubmittedAt(null)
        withDecision(null)
      },
    )

    val (applicationWithInProgressAssessment, _) = createApplication(
      assessmentConfigBlock = {
        withData("{\"some\":\"data\"}")
        withSubmittedAt(null)
        withDecision(null)
      },
    )

    val (applicationWithApprovedAssessment, _) = createApplication(
      assessmentConfigBlock = {
        withData("{\"some\":\"data\"}")
        withSubmittedAt(OffsetDateTime.now())
        withDecision(AssessmentDecision.ACCEPTED)
      },
    )

    val (applicationWithRejectedAssessment, _) = createApplication(
      assessmentConfigBlock = {
        withData("{\"some\":\"data\"}")
        withSubmittedAt(OffsetDateTime.now())
        withDecision(AssessmentDecision.REJECTED)
      },
    )

    val (applicationWithBooking, applicationWithBookingAssessment) = createApplication(
      assessmentConfigBlock = {
        withData("{\"some\":\"data\"}")
        withSubmittedAt(OffsetDateTime.now())
        withDecision(AssessmentDecision.ACCEPTED)
      },
    )

    createBookingForApplicationAndAssessment(applicationWithBooking, applicationWithBookingAssessment!!)

    migrationJobService.runMigrationJob(MigrationJobType.applicationStatuses)

    assertThat(reloadApplication(applicationWithNoAssessment).status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)
    assertThat(reloadApplication(applicationWithAssessmentWithNoData).status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
    assertThat(reloadApplication(applicationWithInProgressAssessment).status).isEqualTo(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
    assertThat(reloadApplication(applicationWithApprovedAssessment).status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    assertThat(reloadApplication(applicationWithRejectedAssessment).status).isEqualTo(ApprovedPremisesApplicationStatus.REJECTED)
    assertThat(reloadApplication(applicationWithBooking).status).isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
  }

  private fun createBookingForApplicationAndAssessment(application: ApprovedPremisesApplicationEntity, assessment: ApprovedPremisesAssessmentEntity) {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(
        probationRegionEntityFactory.produceAndPersist {
          withApArea(apAreaEntityFactory.produceAndPersist())
        },
      )
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withApplication(application)
      withBed(bed)
      withPremises(premises)
    }

    val placementRequirements = placementRequirementsFactory.produceAndPersist {
      withApplication(application)
      withAssessment(assessment)
      withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
      withDesirableCriteria(
        characteristicEntityFactory.produceAndPersistMultiple(5),
      )
      withEssentialCriteria(
        characteristicEntityFactory.produceAndPersistMultiple(3),
      )
    }

    placementRequestFactory.produceAndPersist {
      withAllocatedToUser(user)
      withBooking(booking)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
    }
  }

  private fun createApplication(assessmentConfigBlock: (ApprovedPremisesAssessmentEntityFactory.() -> Unit)? = null): Pair<ApprovedPremisesApplicationEntity, ApprovedPremisesAssessmentEntity?> {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(schema)
      withCreatedByUser(user)
    }
    var assessment: ApprovedPremisesAssessmentEntity? = null

    if (assessmentConfigBlock != null) {
      val assessmentFactory = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .withAssessmentSchema(assessmentSchema)
        .withAllocatedToUser(user)
      assessmentConfigBlock(assessmentFactory)

      assessment = approvedPremisesAssessmentRepository.saveAndFlush(
        assessmentFactory.produce(),
      )
    }

    return Pair(application, assessment)
  }

  private fun reloadApplication(applicationEntity: ApprovedPremisesApplicationEntity): ApprovedPremisesApplicationEntity {
    return approvedPremisesApplicationRepository.findByIdOrNull(applicationEntity.id)!!
  }
}
