package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Probation Region`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelinessEntityRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ApplicationTimelinessTest : IntegrationTestBase() {
  @Autowired
  lateinit var applicationTimelinessEntityRepository: ApplicationTimelinessEntityRepository

  @Test
  fun `it returns application timeliness data`() {
    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val user = userEntityFactory.produceAndPersist {
      withProbationRegion(`Given a Probation Region`())
    }

    val month = 4
    val year = 2022

    val submittedDate = OffsetDateTime.of(LocalDate.of(year, month, 22), LocalTime.MIDNIGHT, ZoneOffset.UTC)

    val risks = PersonRisksFactory()
      .withTier(
        RiskWithStatus(
          RiskTier(
            level = "A1",
            lastUpdated = LocalDate.now(),
          ),
        ),
      ).produce()

    val submittedAndBookedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(submittedDate)
      withRiskRatings(risks)
    }

    val submittedAndAssessedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(submittedDate)
      withRiskRatings(risks)
    }

    val submittedAndUnassessedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(submittedDate)
      withRiskRatings(risks)
    }

    val unSubmittedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
      withSubmittedAt(null)
      withRiskRatings(risks)
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndBookedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      withOccurredAt(submittedDate)
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndAssessedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      withOccurredAt(submittedDate)
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndUnassessedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      withOccurredAt(submittedDate)
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndBookedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      withOccurredAt(submittedDate.plusDays(3))
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndAssessedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      withOccurredAt(submittedDate.plusDays(4))
    }

    domainEventFactory.produceAndPersist {
      withApplicationId(submittedAndBookedApplication.id)
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      withOccurredAt(submittedDate.plusDays(22))
    }

    val result = applicationTimelinessEntityRepository.findAllForMonthAndYear(month, year)

    assertThat(result.size).isEqualTo(3)

    val submittedAndBookedApplicationTimelinessEntity = result.find { it.getId() == submittedAndBookedApplication.id.toString() }
    assertThat(submittedAndBookedApplicationTimelinessEntity).isNotNull()
    assertThat(submittedAndBookedApplicationTimelinessEntity!!.getTier()).isEqualTo("A1")
    assertThat(submittedAndBookedApplicationTimelinessEntity.getApplicationSubmittedAt()).isEqualTo(submittedDate.toInstant())
    assertThat(submittedAndBookedApplicationTimelinessEntity.getBookingMadeAt()).isEqualTo(submittedDate.plusDays(22).toInstant())
    assertThat(submittedAndBookedApplicationTimelinessEntity.getOverallTimeliness()).isEqualTo(22)
    assertThat(submittedAndBookedApplicationTimelinessEntity.getPlacementMatchingTimeliness()).isEqualTo(19)

    val submittedAndAssessedApplicationTimelinessEntity = result.find { it.getId() == submittedAndAssessedApplication.id.toString() }
    assertThat(submittedAndAssessedApplicationTimelinessEntity).isNotNull()
    assertThat(submittedAndAssessedApplicationTimelinessEntity!!.getTier()).isEqualTo("A1")
    assertThat(submittedAndAssessedApplicationTimelinessEntity.getApplicationSubmittedAt()).isEqualTo(submittedDate.toInstant())
    assertThat(submittedAndAssessedApplicationTimelinessEntity.getBookingMadeAt()).isNull()
    assertThat(submittedAndAssessedApplicationTimelinessEntity.getOverallTimeliness()).isNull()
    assertThat(submittedAndAssessedApplicationTimelinessEntity.getPlacementMatchingTimeliness()).isNull()

    val submittedAndUnassessedApplicationTimelinessEntity = result.find { it.getId() == submittedAndUnassessedApplication.id.toString() }
    assertThat(submittedAndUnassessedApplicationTimelinessEntity).isNotNull()
    assertThat(submittedAndUnassessedApplicationTimelinessEntity!!.getTier()).isEqualTo("A1")
    assertThat(submittedAndUnassessedApplicationTimelinessEntity.getApplicationSubmittedAt()).isEqualTo(submittedDate.toInstant())
    assertThat(submittedAndUnassessedApplicationTimelinessEntity.getBookingMadeAt()).isNull()
    assertThat(submittedAndUnassessedApplicationTimelinessEntity.getOverallTimeliness()).isNull()
    assertThat(submittedAndUnassessedApplicationTimelinessEntity.getPlacementMatchingTimeliness()).isNull()
  }
}
