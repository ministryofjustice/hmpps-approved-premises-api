package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesJdbcRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1BedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OccupancyReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesServiceTest {

  @MockK
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @MockK
  lateinit var premisesService: PremisesService

  @MockK
  lateinit var outOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  lateinit var spacePlanningService: SpacePlanningService

  @MockK
  lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  lateinit var bedRepository: BedRepository

  @MockK
  lateinit var cas1BedsRepository: Cas1BedsRepository

  @MockK
  lateinit var cas1OccupancyReportRepository: Cas1OccupancyReportRepository

  @MockK
  lateinit var cas1PremisesLocalRestrictionRepository: Cas1PremisesLocalRestrictionRepository

  @MockK
  lateinit var clock: Clock

  @MockK
  lateinit var premisesJdbcRepository: ApprovedPremisesJdbcRepository

  @InjectMockKs
  lateinit var service: Cas1PremisesService

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.fromString("98fe68f2-a58e-4e55-9d0e-f40856ccde24")
  }

  @Nested
  inner class GetPremisesInfo {

    @Test
    fun `premises not found return error`() {
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremisesInfo(PREMISES_ID)

      assertThatCasResult(result).isNotFound("premises", PREMISES_ID)
    }

    @Test
    fun success() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      val premisesCapacitySummary = PremiseCapacity(
        premisesId = PREMISES_ID,
        range = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(12)),
        byDay = emptyList(),
      )

      val restriction1 = Cas1PremisesLocalRestrictionEntity(
        id = UUID.randomUUID(),
        description = "restriction1",
        createdAt = OffsetDateTime.now(),
        createdByUserId = UUID.randomUUID(),
        approvedPremisesId = PREMISES_ID,
        archived = false,
      )
      val restriction2 = Cas1PremisesLocalRestrictionEntity(
        id = UUID.randomUUID(),
        description = "restriction2",
        createdAt = OffsetDateTime.now(),
        createdByUserId = UUID.randomUUID(),
        approvedPremisesId = PREMISES_ID,
        archived = false,
      )

      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
      every { spacePlanningService.capacity(PREMISES_ID, any(), null) } returns premisesCapacitySummary
      every { spaceBookingRepository.countActiveSpaceBookings(PREMISES_ID) } returns 4
      every {
        cas1PremisesLocalRestrictionRepository.findAllByApprovedPremisesIdAndArchivedFalseOrderByCreatedAtDesc(
          PREMISES_ID,
        )
      } returns listOf(restriction1, restriction2)
      every { premisesJdbcRepository.findAllCharacteristicPropertyNames(PREMISES_ID) } returns listOf("char1", "char2")

      val result = service.getPremisesInfo(PREMISES_ID)

      assertThatCasResult(result).isSuccess().with {
        val premisesSummaryInfo = it
        assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
        assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
        assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
        assertThat(premisesSummaryInfo.availableBeds).isEqualTo(48)
        assertThat(premisesSummaryInfo.localRestrictions).isEqualTo(
          listOf(
            Cas1PremisesLocalRestrictionSummary(
              restriction1.id,
              restriction1.description,
              restriction1.createdAt.toLocalDate(),
            ),
            Cas1PremisesLocalRestrictionSummary(
              restriction2.id,
              restriction2.description,
              restriction2.createdAt.toLocalDate(),
            ),
          ),
        )
        assertThat(premisesSummaryInfo.characteristicPropertyNames).containsExactly("char1", "char2")
      }
    }
  }

  @Nested
  inner class GetPremisesCapacities {

    @Test
    fun `at least one premises not found return error`() {
      every { approvedPremisesRepository.findAllById(listOf(PREMISES_ID)) } returns emptyList()

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThatCasResult(result).isGeneralValidationError("Could not resolve all premises IDs. Missing IDs are [98fe68f2-a58e-4e55-9d0e-f40856ccde24]")
    }

    @Test
    fun `start before end date return error`() {
      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(ApprovedPremisesEntityFactory().withDefaults().produce())

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 4),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThatCasResult(result).isGeneralValidationError("Start Date 2020-01-04 should be before End Date 2020-01-03")
    }

    @Test
    fun `success, no premises requested`() {
      val result = service.getPremisesCapacities(
        premisesIds = emptyList(),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
        assertThat(it.endDate).isEqualTo(LocalDate.of(2020, 1, 3))
        assertThat(it.results).isEmpty()
      }
    }

    @Test
    fun `success, single premises`() {
      val premise = ApprovedPremisesEntityFactory().withDefaults().withId(PREMISES_ID).produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(premise)

      val capacityResponse = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(PREMISES_ID),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2020, 1, 3),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
        assertThat(it.endDate).isEqualTo(LocalDate.of(2020, 1, 3))
        assertThat(it.results).isEqualTo(listOf(capacityResponse))
      }
    }

    @Test
    fun `success, multiple premises`() {
      val premises1 = ApprovedPremisesEntityFactory().withDefaults().produce()
      val premises2 = ApprovedPremisesEntityFactory().withDefaults().produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(premises1.id, premises2.id))
      } returns listOf(premises1, premises2)

      val capacityResponse1 = mockk<PremiseCapacity>()
      val capacityResponse2 = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(premises1.id, premises2.id),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 2, 2),
            LocalDate.of(2020, 2, 3),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse1, capacityResponse2)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(premises1.id, premises2.id),
        startDate = LocalDate.of(2020, 2, 2),
        endDate = LocalDate.of(2020, 2, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.results).isEqualTo(listOf(capacityResponse1, capacityResponse2))
      }
    }

    @Test
    fun `success truncate to less than 2 years`() {
      val premise = ApprovedPremisesEntityFactory().withDefaults().withId(PREMISES_ID).produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every {
        approvedPremisesRepository.findAllById(listOf(PREMISES_ID))
      } returns listOf(premise)

      val capacityResponse = mockk<PremiseCapacity>()

      every {
        spacePlanningService.capacity(
          forPremisesIds = listOf(PREMISES_ID),
          rangeInclusive = DateRange(
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2022, 1, 1),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns listOf(capacityResponse)

      val result = service.getPremisesCapacities(
        premisesIds = listOf(PREMISES_ID),
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2023, 1, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.results).isEqualTo(listOf(capacityResponse))
      }
    }
  }

  @Nested
  inner class PremisesLocalRestrictions {

    @Test
    fun `successfully add restriction`() {
      val userID = UUID.randomUUID()
      val description = "description"
      val restrictionCaptor = slot<Cas1PremisesLocalRestrictionEntity>()

      every { cas1PremisesLocalRestrictionRepository.saveAndFlush(capture(restrictionCaptor)) } answers { restrictionCaptor.captured }

      service.addLocalRestriction(
        premisesId = PREMISES_ID,
        createdByUserId = userID,
        description = description,
      )

      verify(exactly = 1) { cas1PremisesLocalRestrictionRepository.saveAndFlush(any()) }
      val savedEntity = restrictionCaptor.captured

      assertEquals(description, savedEntity.description)
      assertEquals(userID, savedEntity.createdByUserId)
      assertEquals(PREMISES_ID, savedEntity.approvedPremisesId)
    }

    @Test
    fun `getLocalRestrictions should return the correct list of restrictions in descending order by createdAt and and exclude archived ones`() {
      val premisesId = UUID.randomUUID()
      val restrictions = listOf(
        Cas1PremisesLocalRestrictionEntity(
          id = UUID.randomUUID(),
          approvedPremisesId = premisesId,
          description = "Restriction 1",
          createdAt = OffsetDateTime.parse("2025-07-17T11:13:55.990520+01:00"), // Older
          archived = false,
          createdByUserId = UUID.randomUUID(),
        ),
        Cas1PremisesLocalRestrictionEntity(
          id = UUID.randomUUID(),
          approvedPremisesId = premisesId,
          description = "Restriction 2",
          createdAt = OffsetDateTime.parse("2025-07-18T11:13:55.990520+01:00"), // Newer
          archived = false,
          createdByUserId = UUID.randomUUID(),
        ),
        Cas1PremisesLocalRestrictionEntity(
          id = UUID.randomUUID(),
          approvedPremisesId = premisesId,
          description = "Restriction 3",
          createdAt = OffsetDateTime.now(),
          archived = true,
          createdByUserId = UUID.randomUUID(),
        ),
      )

      every {
        cas1PremisesLocalRestrictionRepository.findAllByApprovedPremisesIdAndArchivedFalseOrderByCreatedAtDesc(premisesId)
      } returns restrictions.filter { !it.archived }.sortedByDescending { it.createdAt }

      val result = service.getLocalRestrictions(premisesId)

      assertThat(result.size).isEqualTo(2)
      assertThat(result.map { it.description }).containsExactly("Restriction 2", "Restriction 1")
    }

    @Test
    fun `deleteLocalRestriction should set the archived property to true for the given restriction`() {
      val premisesId = UUID.randomUUID()
      val restrictionId = UUID.randomUUID()
      val restriction = Cas1PremisesLocalRestrictionEntity(
        id = restrictionId,
        approvedPremisesId = premisesId,
        description = "Active Restriction",
        createdAt = OffsetDateTime.now(),
        archived = false,
        createdByUserId = UUID.randomUUID(),
      )

      every {
        cas1PremisesLocalRestrictionRepository.findByIdOrNull(restrictionId)
      } returns restriction

      every {
        cas1PremisesLocalRestrictionRepository.save(any())
      } returnsArgument 0

      service.deleteLocalRestriction(premisesId, restrictionId)

      val updatedRestriction = slot<Cas1PremisesLocalRestrictionEntity>()
      verify {
        cas1PremisesLocalRestrictionRepository.save(capture(updatedRestriction))
      }
      with(updatedRestriction.captured) {
        assertThat(archived).isTrue()
      }
    }
  }
}
