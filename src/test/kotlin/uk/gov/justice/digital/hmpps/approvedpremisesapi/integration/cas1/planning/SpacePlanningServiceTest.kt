package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.planning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.OutOfServiceBedRevision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBedWithMultipleRevisions
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Because the [SpacePlanningService] is mostly 'glue' code between the database and the [SpacePlanningModelsFactory],
 * it is exclusively tested using integration tests
 */
class SpacePlanningServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var spacePlanner: SpacePlanningService

  @Nested
  inner class Capacity {

    lateinit var premises1: ApprovedPremisesEntity
    lateinit var premises2: ApprovedPremisesEntity
    lateinit var premises3: ApprovedPremisesEntity

    private lateinit var premises1BookingCrn1: Cas1SpaceBookingEntity

    /**
     * premises 1 room 1 bed 1 - active, despite a past OOSB record (with amendments)
     * premises 1 room 2 bed 1 - active, despite an upcoming OOSB record
     * premises 1 room 3 bed 1 - active, despite a cancelled OOSB record
     * premises 1 room 3 bed 2 - active
     * premises 1 room 3 bed 3 - out of service between 7th and 8th
     * premises 1 room 3 bed 4 - bed ended
     * premises 2 room 1 bed 1 - active
     * premises 2 room 2 bed 1 - out of service between 9th and 11th
     * premises 3 room 1 bed 1 - active (but should never be returned as premise 3 not requested)
     */
    @BeforeEach
    fun setupTestData() {
      premises1 = givenAnApprovedPremises(name = "Premises 1")
      premises2 = givenAnApprovedPremises(name = "Premises 2")
      premises3 = givenAnApprovedPremises(name = "Premises 3")

      val premises1Room1 = createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_ARSON_SUITABLE, CAS1_PROPERTY_NAME_ENSUITE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 1 - Room 1 - Bed 1")),
      )

      givenAnOutOfServiceBed(
        bed = premises1Room1.beds.first { it.name == "Premises 1 - Room 1 - Bed 1" },
        startDate = date(2020, 5, 1),
        endDate = date(2020, 5, 5),
        reason = "ignored, before requested period",
      )

      val premises1Room2 = createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 1 - Room 2 - Bed 1")),
      )

      givenAnOutOfServiceBedWithMultipleRevisions(
        bed = premises1Room2.beds.first { it.name == "Premises 1 - Room 2 - Bed 1" },
        revisions = listOf(
          OutOfServiceBedRevision(
            createdAt = OffsetDateTime.now().minusDays(10),
            startDate = date(2020, 1, 1),
            endDate = date(2020, 12, 1),
            reason = "initial revision in scope, ignored as have subsequent revision",
          ),
          OutOfServiceBedRevision(
            createdAt = OffsetDateTime.now().minusDays(5),
            startDate = date(2020, 5, 11),
            endDate = date(2020, 5, 15),
            reason = "ignored, after requested period",
          ),
        ),
      )

      val premises1Room3 = createRoom(
        premises = premises1,
        characteristicPropertyNames = emptyList(),
        beds = listOf(
          RequiredBed("Premises 1 - Room 3 - Bed 1 - Cancelled OOSB"),
          RequiredBed("Premises 1 - Room 3 - Bed 2"),
          RequiredBed("Premises 1 - Room 3 - Bed 3 - OOSB"),
          RequiredBed("Premises 1 - Room 3 - Bed 4 - Ended", endDate = date(2020, 5, 8)),
        ),
      )

      givenAnOutOfServiceBed(
        bed = premises1Room3.beds.first { it.name == "Premises 1 - Room 3 - Bed 1 - Cancelled OOSB" },
        startDate = date(2010, 1, 1),
        endDate = date(2090, 1, 1),
        reason = "ignored, because cancelled",
        cancelled = true,
      )

      givenAnOutOfServiceBed(
        bed = premises1Room3.beds.first { it.name == "Premises 1 - Room 3 - Bed 3 - OOSB" },
        startDate = date(2020, 5, 7),
        endDate = date(2020, 5, 8),
        reason = "refurb",
      )

      createRoom(
        premises = premises2,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED, CAS1_PROPERTY_NAME_ENSUITE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 2 - Room 1 - Bed 1")),
      )

      val premises2Room2 = createRoom(
        premises = premises2,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_ARSON_SUITABLE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 2 - Room 2 - Bed 1 - OOSB")),
      )

      givenAnOutOfServiceBed(
        bed = premises2Room2.beds.first { it.name == "Premises 2 - Room 2 - Bed 1 - OOSB" },
        startDate = date(2020, 5, 9),
        endDate = date(2020, 5, 11),
        reason = "painting",
      )

      createRoom(
        premises = premises3,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED, CAS1_PROPERTY_NAME_ENSUITE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 3 - Room 1 - Bed 1")),
      )

      premises1BookingCrn1 = createSpaceBooking(crn = "PREMISES1-CRN1") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 4))
        withCanonicalDepartureDate(date(2020, 5, 11))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ARSON_SUITABLE),
        )
      }

      createSpaceBooking(crn = "PREMISES1-CRN2") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 6))
        withCanonicalDepartureDate(date(2020, 5, 9))
      }

      createSpaceBooking(crn = "PREMISES1-CRN3") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 7))
        withCanonicalDepartureDate(date(2020, 5, 20))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      createSpaceBooking(crn = "PREMISES1-CRN4") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 29))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      createSpaceBooking(crn = "PREMISES1-CRN5") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 9))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }

      createSpaceBooking(crn = "PREMISES1-OUTOFRANGEBEFORE") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 5))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }

      createSpaceBooking(crn = "PREMISES1-OUTOFRANGEAFTER") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 11))
        withCanonicalDepartureDate(date(2020, 5, 12))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }

      createSpaceBooking(crn = "PREMISES2-CRN10") {
        withPremises(premises2)
        withCanonicalArrivalDate(date(2020, 4, 1))
        withCanonicalDepartureDate(date(2020, 6, 29))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      createSpaceBooking(crn = "PREMISES2-CRN11") {
        withPremises(premises2)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 9))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }
    }

    @Test
    fun `single premises success`() {
      val capacity = spacePlanner.capacity(
        premisesId = premises1.id,
        rangeInclusive = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
        excludeSpaceBookingId = null,
      )

      assertThat(capacity.premisesId).isEqualTo(premises1.id)
      assertPremises1CapacityNoExclusions(capacity)
    }

    @Test
    fun `single premises success, excluding a space booking`() {
      val capacity = spacePlanner.capacity(
        premisesId = premises1.id,
        rangeInclusive = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
        excludeSpaceBookingId = premises1BookingCrn1.id,
      )

      assertThat(capacity.premisesId).isEqualTo(premises1.id)
      assertThat(capacity.byDay).hasSize(5)

      val day1Capacity = capacity.byDay[0]
      assertThat(day1Capacity.day).isEqualTo(date(2020, 5, 6))
      assertThat(day1Capacity.bookingCount).isEqualTo(3)
      assertThat(day1Capacity.totalBedCount).isEqualTo(6)
      assertThat(day1Capacity.availableBedCount).isEqualTo(6)

      assertThat(day1Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day2Capacity = capacity.byDay[1]
      assertThat(day2Capacity.day).isEqualTo(date(2020, 5, 7))
      assertThat(day2Capacity.bookingCount).isEqualTo(4)
      assertThat(day2Capacity.totalBedCount).isEqualTo(6)
      assertThat(day2Capacity.availableBedCount).isEqualTo(5)

      assertThat(day2Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day3Capacity = capacity.byDay[2]
      assertThat(day3Capacity.day).isEqualTo(date(2020, 5, 8))
      assertThat(day3Capacity.bookingCount).isEqualTo(4)
      assertThat(day3Capacity.totalBedCount).isEqualTo(5)
      assertThat(day3Capacity.availableBedCount).isEqualTo(4)

      assertThat(day3Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day4Capacity = capacity.byDay[3]
      assertThat(day4Capacity.day).isEqualTo(date(2020, 5, 9))
      assertThat(day4Capacity.bookingCount).isEqualTo(2)
      assertThat(day4Capacity.totalBedCount).isEqualTo(5)
      assertThat(day4Capacity.availableBedCount).isEqualTo(5)

      assertThat(day4Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 0),
      )

      val day5Capacity = capacity.byDay[4]
      assertThat(day5Capacity.day).isEqualTo(date(2020, 5, 10))
      assertThat(day5Capacity.bookingCount).isEqualTo(2)
      assertThat(day5Capacity.totalBedCount).isEqualTo(5)
      assertThat(day5Capacity.availableBedCount).isEqualTo(5)
    }

    @Test
    fun `multiple premises success`() {
      val capacity = spacePlanner.capacity(
        forPremisesIds = listOf(premises1.id, premises2.id),
        rangeInclusive = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
        excludeSpaceBookingId = null,
      )

      assertPremises1CapacityNoExclusions(capacity.first { it.premisesId == premises1.id })
      assertPremises2CapacityNoExclusions(capacity.first { it.premisesId == premises2.id })
    }

    private fun assertPremises2CapacityNoExclusions(capacity: PremiseCapacity) {
      assertThat(capacity.byDay).hasSize(5)

      val day1Capacity = capacity.byDay[0]
      assertThat(day1Capacity.day).isEqualTo(date(2020, 5, 6))
      assertThat(day1Capacity.bookingCount).isEqualTo(2)
      assertThat(day1Capacity.totalBedCount).isEqualTo(2)
      assertThat(day1Capacity.availableBedCount).isEqualTo(2)
      assertThat(day1Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day2Capacity = capacity.byDay[1]
      assertThat(day2Capacity.day).isEqualTo(date(2020, 5, 7))
      assertThat(day2Capacity.bookingCount).isEqualTo(2)
      assertThat(day2Capacity.totalBedCount).isEqualTo(2)
      assertThat(day2Capacity.availableBedCount).isEqualTo(2)
      assertThat(day2Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day3Capacity = capacity.byDay[2]
      assertThat(day3Capacity.day).isEqualTo(date(2020, 5, 8))
      assertThat(day3Capacity.bookingCount).isEqualTo(2)
      assertThat(day3Capacity.totalBedCount).isEqualTo(2)
      assertThat(day3Capacity.availableBedCount).isEqualTo(2)
      assertThat(day3Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day4Capacity = capacity.byDay[3]
      assertThat(day4Capacity.day).isEqualTo(date(2020, 5, 9))
      assertThat(day4Capacity.bookingCount).isEqualTo(1)
      assertThat(day4Capacity.totalBedCount).isEqualTo(2)
      assertThat(day4Capacity.availableBedCount).isEqualTo(1)
      assertThat(day4Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 0),
      )

      val day5Capacity = capacity.byDay[4]
      assertThat(day5Capacity.day).isEqualTo(date(2020, 5, 10))
      assertThat(day5Capacity.bookingCount).isEqualTo(1)
      assertThat(day5Capacity.totalBedCount).isEqualTo(2)
      assertThat(day5Capacity.availableBedCount).isEqualTo(1)
      assertThat(day5Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 0),
      )
    }

    private fun assertPremises1CapacityNoExclusions(capacity: PremiseCapacity) {
      assertThat(capacity.byDay).hasSize(5)

      val day1Capacity = capacity.byDay[0]
      assertThat(day1Capacity.day).isEqualTo(date(2020, 5, 6))
      assertThat(day1Capacity.bookingCount).isEqualTo(4)
      assertThat(day1Capacity.totalBedCount).isEqualTo(6)
      assertThat(day1Capacity.availableBedCount).isEqualTo(6)
      assertThat(day1Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 1),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day2Capacity = capacity.byDay[1]
      assertThat(day2Capacity.day).isEqualTo(date(2020, 5, 7))
      assertThat(day2Capacity.bookingCount).isEqualTo(5)
      assertThat(day2Capacity.totalBedCount).isEqualTo(6)
      assertThat(day2Capacity.availableBedCount).isEqualTo(5)
      assertThat(day2Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day3Capacity = capacity.byDay[2]
      assertThat(day3Capacity.day).isEqualTo(date(2020, 5, 8))
      assertThat(day3Capacity.bookingCount).isEqualTo(5)
      assertThat(day3Capacity.totalBedCount).isEqualTo(5)
      assertThat(day3Capacity.availableBedCount).isEqualTo(4)
      assertThat(day3Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 1),
      )

      val day4Capacity = capacity.byDay[3]
      assertThat(day4Capacity.day).isEqualTo(date(2020, 5, 9))
      assertThat(day4Capacity.bookingCount).isEqualTo(3)
      assertThat(day4Capacity.totalBedCount).isEqualTo(5)
      assertThat(day4Capacity.availableBedCount).isEqualTo(5)
      assertThat(day4Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 0),
      )

      val day5Capacity = capacity.byDay[4]
      assertThat(day5Capacity.day).isEqualTo(date(2020, 5, 10))
      assertThat(day5Capacity.bookingCount).isEqualTo(3)
      assertThat(day5Capacity.totalBedCount).isEqualTo(5)
      assertThat(day5Capacity.availableBedCount).isEqualTo(5)
      assertThat(day5Capacity.characteristicAvailability).containsExactly(
        PremiseCharacteristicAvailability("isArsonSuitable", availableBedCount = 1, bookingCount = 1),
        PremiseCharacteristicAvailability("hasEnSuite", availableBedCount = 1, bookingCount = 0),
        PremiseCharacteristicAvailability("isSingle", availableBedCount = 2, bookingCount = 2),
        PremiseCharacteristicAvailability("isStepFreeDesignated", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isSuitedForSexOffenders", availableBedCount = 0, bookingCount = 0),
        PremiseCharacteristicAvailability("isWheelchairDesignated", availableBedCount = 0, bookingCount = 0),
      )
    }
  }

  private fun findCharacteristic(propertyName: String) = characteristicRepository.findByPropertyName(propertyName, ServiceName.approvedPremises.value)!!

  private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

  private fun createSpaceBooking(
    crn: String,
    configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
  ): Cas1SpaceBookingEntity {
    val (user) = givenAUser()
    val (placementRequest) = givenAPlacementRequest(
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
    )

    return cas1SpaceBookingEntityFactory.produceAndPersist {
      withCrn(crn)
      withPlacementRequest(placementRequest)
      withApplication(placementRequest.application)
      withCreatedBy(user)

      configuration.invoke(this)
    }
  }

  data class RequiredBed(val name: String, val endDate: LocalDate? = null)

  private fun createRoom(
    premises: ApprovedPremisesEntity,
    characteristicPropertyNames: List<String>,
    beds: List<RequiredBed>,
  ): RoomEntity {
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withCharacteristics(characteristicPropertyNames.map { findCharacteristic(it) }.toMutableList())
    }

    premises.rooms.add(room)

    beds.forEach { bed ->
      room.beds.add(
        bedEntityFactory.produceAndPersist {
          withName(bed.name)
          withEndDate { bed.endDate }
          withYieldedRoom { room }
        },
      )
    }

    return room
  }
}
