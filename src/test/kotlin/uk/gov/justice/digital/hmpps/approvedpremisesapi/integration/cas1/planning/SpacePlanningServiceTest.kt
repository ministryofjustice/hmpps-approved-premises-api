package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.planning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanRenderer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

/**
 * Because the [SpacePlanningService] is mostly 'glue' code between the database and the [SpacePlanningModelsFactory],
 * it is exclusively tested using integration tests
 */
class SpacePlanningServiceTest : IntegrationTestBase() {

  @Autowired
  lateinit var spacePlanner: SpacePlanningService

  @Nested
  inner class Plan {

    lateinit var premises1: ApprovedPremisesEntity

    @BeforeEach
    fun setupTestData() {
      premises1 = givenAnApprovedPremises(
        id = UUID.fromString("0f9384e2-2f94-41d9-a79c-4e35e67111ec"),
        name = "Premises 1",
      )

      createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_ARSON_SUITABLE, CAS1_PROPERTY_NAME_ENSUITE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Room 1 - Bed 1")),
      )

      createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Room 2 - Bed 1")),
      )

      val premises1Room3 = createRoom(
        premises = premises1,
        characteristicPropertyNames = emptyList(),
        beds = listOf(
          RequiredBed("Room 3 - Bed 1"),
          RequiredBed("Room 3 - Bed 2"),
          RequiredBed("Room 3 - Bed 3"),
          RequiredBed("Room 3 - Bed 4", endDate = date(2020, 5, 8)),
        ),
      )

      givenAnOutOfServiceBed(
        bed = premises1Room3.beds.first { it.name == "Room 3 - Bed 3" },
        startDate = date(2020, 5, 7),
        endDate = date(2020, 5, 8),
        reason = "refurb",
      )

      createSpaceBooking(crn = "CRN1") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 4))
        withCanonicalDepartureDate(date(2020, 5, 11))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ARSON_SUITABLE),
        )
      }

      createSpaceBooking(crn = "CRN2") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 6))
        withCanonicalDepartureDate(date(2020, 5, 9))
      }

      createSpaceBooking(crn = "CRN3") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 7))
        withCanonicalDepartureDate(date(2020, 5, 20))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      createSpaceBooking(crn = "CRN4") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 29))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      createSpaceBooking(crn = "CRN5") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 9))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }

      createSpaceBooking(crn = "OUTOFRANGEBEFORE") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 1))
        withCanonicalDepartureDate(date(2020, 5, 5))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }

      createSpaceBooking(crn = "OUTOFRANGEAFTER") {
        withPremises(premises1)
        withCanonicalArrivalDate(date(2020, 5, 11))
        withCanonicalDepartureDate(date(2020, 5, 12))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
        )
      }
    }

    @Test
    fun success() {
      val criteria = SpacePlanningService.PlanCriteria(
        premises = premises1,
        range = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
      )

      val plan = spacePlanner.plan(criteria)

      assertThat(
        SpacePlanRenderer.render(criteria, plan),
      ).isEqualTo(
        """
        Space Plan for Premises 1 (0f9384e2-2f94-41d9-a79c-4e35e67111ec) from 2020-05-06 to 2020-05-10
        
        There are 3 days with unplanned bookings:
        
        2020-05-06 has 1
        2020-05-07 has 2
        2020-05-08 has 2
        
        | Room (6)             | **2020-05-06**<br/>capacity: 6<br/>planned: 3<br/>unplanned: 1         | **2020-05-07**<br/>capacity: 5<br/>planned: 3<br/>unplanned: 2         | **2020-05-08**<br/>capacity: 4<br/>planned: 3<br/>unplanned: 2         | **2020-05-09**<br/>capacity: 5<br/>planned: 3<br/>unplanned: 0         | **2020-05-10**<br/>capacity: 5<br/>planned: 3<br/>unplanned: 0         |
        | -------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------- |
        | **Room 1 - Bed 1**<br/>hasEnSuite<br/>isArsonSuitable<br/>isSingle | **CRN1**<br/>hasEnSuite(r)<br/>isArsonSuitable(rb)<br/>isSingle(r)     | **CRN1**<br/>hasEnSuite(r)<br/>isArsonSuitable(rb)<br/>isSingle(r)     | **CRN1**<br/>hasEnSuite(r)<br/>isArsonSuitable(rb)<br/>isSingle(r)     | **CRN1**<br/>hasEnSuite(r)<br/>isArsonSuitable(rb)<br/>isSingle(r)     | **CRN1**<br/>hasEnSuite(r)<br/>isArsonSuitable(rb)<br/>isSingle(r)     |
        | **Room 2 - Bed 1**<br/>isSingle | **CRN4**<br/>isSingle(rb)                                              | **CRN3**<br/>isSingle(rb)                                              | **CRN3**<br/>isSingle(rb)                                              | **CRN3**<br/>isSingle(rb)                                              | **CRN3**<br/>isSingle(rb)                                              |
        | **Room 3 - Bed 1**   | **CRN2**                                                               | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               |
        | **Room 3 - Bed 2**   |                                                                        | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               |
        | **Room 3 - Bed 3**   |                                                                        | OOSB refurb                                                            | OOSB refurb                                                            | **CRN4**<br/>isSingle(b)                                               | **CRN4**<br/>isSingle(b)                                               |
        | **Room 3 - Bed 4**   |                                                                        | **CRN4**<br/>isSingle(b)                                               | Bed Ended 2020-05-08                                                   | Bed Ended 2020-05-08                                                   | Bed Ended 2020-05-08                                                   |
        | **unplanned**        | **CRN5**<br/>hasEnSuite<br/>isWheelchairDesignated                     | **CRN5**<br/>hasEnSuite<br/>isWheelchairDesignated<br/><br/>**CRN2**   | **CRN5**<br/>hasEnSuite<br/>isWheelchairDesignated<br/><br/>**CRN2**   |                                                                        |                                                                        |
        """.trimIndent(),
      )
    }
  }

  @Nested
  inner class Capacity {

    lateinit var premises1: ApprovedPremisesEntity

    private lateinit var premises1BookingCrn1: Cas1SpaceBookingEntity

    @BeforeEach
    fun setupTestData() {
      premises1 = givenAnApprovedPremises(name = "Premises 1")

      createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_ARSON_SUITABLE, CAS1_PROPERTY_NAME_ENSUITE, CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 1 - Room 1 - Bed 1")),
      )

      createRoom(
        premises = premises1,
        characteristicPropertyNames = listOf(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        beds = listOf(RequiredBed("Premises 1 - Room 2 - Bed 1")),
      )

      val premises1Room3 = createRoom(
        premises = premises1,
        characteristicPropertyNames = emptyList(),
        beds = listOf(
          RequiredBed("Premises 1 - Room 3 - Bed 1"),
          RequiredBed("Premises 1 - Room 3 - Bed 2"),
          RequiredBed("Premises 1 - Room 3 - Bed 3"),
          RequiredBed("Premises 1 - Room 3 - Bed 4", endDate = date(2020, 5, 8)),
        ),
      )

      givenAnOutOfServiceBed(
        bed = premises1Room3.beds.first { it.name == "Premises 1 - Room 3 - Bed 3" },
        startDate = date(2020, 5, 7),
        endDate = date(2020, 5, 8),
        reason = "refurb",
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
    }

    @Test
    fun success() {
      val capacity = spacePlanner.capacity(
        premises = premises1,
        rangeInclusive = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
        excludeSpaceBookingId = null,
      )

      assertThat(capacity.premise.id).isEqualTo(premises1.id)
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
    }

    @Test
    fun `success, excluding a space booking`() {
      val capacity = spacePlanner.capacity(
        premises = premises1,
        rangeInclusive = DateRange(
          fromInclusive = date(2020, 5, 6),
          toInclusive = date(2020, 5, 10),
        ),
        excludeSpaceBookingId = premises1BookingCrn1.id,
      )

      assertThat(capacity.premise.id).isEqualTo(premises1.id)
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
