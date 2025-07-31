package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS3_REPORTER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class PremisesTest {
  @Nested
  inner class CreatePremises : InitialiseDatabasePerClassTestBase() {
    private lateinit var user: UserEntity
    private lateinit var probationDeliveryUnit: ProbationDeliveryUnitEntity
    private lateinit var jwt: String

    @BeforeAll
    fun setup() {
      val userArgs = givenAUser()

      user = userArgs.first
      jwt = userArgs.second
      probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }
    }

    @Test
    fun `Create new premises returns 201`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "test-premises",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
    }

    @Test
    fun `When a new premises is created then all field data is persisted`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "new premises",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
            turnaroundWorkingDayCount = 5,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("new premises")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
    }

    @Test
    fun `When a new Temporary Accommodation premises is created with a legacy PDU name then all field data is persisted`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "legacy pdu name",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = probationDeliveryUnit.name,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("legacy pdu name")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
    }

    @Test
    fun `Trying to create a new premises without a name returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `When a new Temporary Accommodation premises is created with no turnaround working day count then it defaults to 2`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises with no turnaround working day count",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("addressLine1").isEqualTo("1 somewhere")
        .jsonPath("addressLine2").isEqualTo("Some district")
        .jsonPath("town").isEqualTo("Somewhere")
        .jsonPath("postcode").isEqualTo("AB123CD")
        .jsonPath("service").isEqualTo(ServiceName.temporaryAccommodation.value)
        .jsonPath("notes").isEqualTo("some arbitrary notes")
        .jsonPath("name").isEqualTo("premises with no turnaround working day count")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(2)
    }

    @ParameterizedTest(name = "Trying to create a new Temporary Accommodation premises with turnaround working day count = {0} returns 400 and errorType = {1}")
    @CsvSource(
      "-1,isNotAPositiveInteger",
      "-4,isNotAPositiveInteger",
    )
    fun `Trying to create a new Temporary Accommodation premises with turnaround working day count less than 1 returns 400`(
      turnaroundWorkingDayCount: Int,
      expectedErrorType: String,
    ) {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "with turnaround working day count less than 1",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
            turnaroundWorkingDayCount = turnaroundWorkingDayCount,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.turnaroundWorkingDayCount")
        .jsonPath("invalid-params[0].errorType").isEqualTo(expectedErrorType)
    }

    @Test
    fun `Trying to create a new premises with a non-unique name returns 400`() {
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { givenAProbationRegion() }
        withName("premises-name-conflict")
      }

      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "premises-name-conflict",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("notUnique")
    }

    @Test
    fun `When a new premises is created with no notes then it defaults to empty`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            name = "Premises with no notes",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
            probationDeliveryUnitId = probationDeliveryUnit.id,
          ),
        )
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("notes").isEqualTo("")
    }

    @Test
    fun `Trying to create a new premises without an address returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "AB123CD",
            addressLine1 = "",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a new premises without a postcode returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "",
            addressLine1 = "FIRST LINE OF THE ADDRESS",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a new premises without a service returns 400`() {
      webTestClient.post()
        .uri("/premises?service=temporary-accommodation")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            postcode = "AB123CD",
            addressLine1 = "FIRST LINE OF THE ADDRESS",
            addressLine2 = "Some district",
            town = "Somewhere",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
            notes = "some notes",
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("onlyCas3Supported")
    }

    @Test
    fun `Trying to create a new premises with an invalid local authority area id returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            // not in db
            localAuthorityAreaId = UUID.fromString("878217f0-6db5-49d8-a5a1-c40fdecd6060"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }

    @Test
    fun `Trying to create a new Temporary Accommodation premises with a probation region that's not the user's region returns 403 Forbidden`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
            // not in db
            probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"),
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises without a PDU returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises without a pdu",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = null,
            probationDeliveryUnitId = null,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
        .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "premises with an invalid probation delivery unit",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = null,
            probationDeliveryUnitId = UUID.randomUUID(),
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }

    @Test
    fun `Trying to create a Temporary Accommodation Premises with an invalid PDU name returns 400`() {
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewPremises(
            addressLine1 = "1 somewhere",
            addressLine2 = "Some district",
            town = "Somewhere",
            postcode = "AB123CD",
            notes = "some arbitrary notes",
            name = "invalid PDU",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            pdu = "Non-existent PDU",
            probationDeliveryUnitId = null,
          ),
        )
        .exchange()
        .expectStatus()
        .is4xxClientError
        .expectBody()
        .jsonPath("title").isEqualTo("Bad Request")
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.pdu")
        .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
    }
  }

  @Nested
  inner class UpdatePremises : IntegrationTestBase() {

    @Test
    fun `When a Temporary Accommodation Premises is updated then all field data is persisted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premisesToGet }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now().plusDays(10) }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)

        assertTemporaryAccommodationPremisesBedSpaceIsArchived(premisesToGet)
      }
    }

    @Test
    fun `When a Temporary Accommodation Premises is updated with online status then respective bedspace enddate is not updated`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premisesToGet }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now() }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("active")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)

        val findById = temporaryAccommodationPremisesRepository.findById(premisesToGet.id)
        assertThat(findById).isNotNull()
        assertThat(findById.get().rooms).isNotEmpty()
        val actualBedWithEndDate = findById.get().rooms.flatMap { it.beds }.firstOrNull { it.id == bedEntityWithEndDate.id }
        val actualBedWithoutEndDate = findById.get().rooms.flatMap { it.beds }.firstOrNull { it.id == bedEntityWithoutEndDate.id }
        assertThat(actualBedWithEndDate!!.endDate).isEqualTo(bedEntityWithEndDate.endDate)
        assertThat(actualBedWithoutEndDate!!.endDate).isEqualTo(bedEntityWithoutEndDate.endDate)
      }
    }

    @Test
    fun `When a Temporary Accommodation Premises is updated with a legacy PDU name then all field data is persisted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = probationDeliveryUnit.name,
              turnaroundWorkingDayCount = 5,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
      }
    }

    @Test
    fun `Updating a Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val otherProbationRegion = givenAProbationRegion()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(otherProbationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(otherProbationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Trying to update a premises with an invalid probation region id returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        webTestClient.put()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // not in db
              probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"),
              characteristicIds = mutableListOf(),
              status = PropertyStatus.active,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises without a PDU returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises with an invalid PDU name returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = "Non-existent PDU",
              probationDeliveryUnitId = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.pdu")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, -5, -10])
    fun `Trying to update a Temporary Accommodation Premises with invalid turnaround working day count = {0} returns 400 and errorType is isNotAPositiveInteger`(
      turnaroundWorkingDayCount: Int,
    ) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = turnaroundWorkingDayCount,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.turnaroundWorkingDayCount")
          .jsonPath("invalid-params[0].errorType").isEqualTo("isNotAPositiveInteger")
      }
    }

    @Test
    fun `Trying to update a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              // North West
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = UUID.randomUUID(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.probationDeliveryUnitId")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation premises does not change the name when it's not provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
          withName("old-premises-name")
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
              name = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
          .jsonPath("name").isEqualTo("old-premises-name")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation premises changes the name when it's provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
          withName("old-premises-name")
        }

        val premisesToGet = premises[0]

        webTestClient.put()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = "1 somewhere updated",
              addressLine2 = "Some other district",
              town = "Somewhere Else",
              postcode = "AB456CD",
              notes = "some arbitrary notes updated",
              // Allerdale
              localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
              probationRegionId = user.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              probationDeliveryUnitId = probationDeliveryUnit.id,
              turnaroundWorkingDayCount = 5,
              name = "new-premises-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addressLine1").isEqualTo("1 somewhere updated")
          .jsonPath("addressLine2").isEqualTo("Some other district")
          .jsonPath("town").isEqualTo("Somewhere Else")
          .jsonPath("postcode").isEqualTo("AB456CD")
          .jsonPath("notes").isEqualTo("some arbitrary notes updated")
          .jsonPath("localAuthorityArea.id").isEqualTo("d1bd139b-7b90-4aae-87aa-9f93e183a7ff")
          .jsonPath("localAuthorityArea.name").isEqualTo("Allerdale")
          .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
          .jsonPath("probationRegion.name").isEqualTo(user.probationRegion.name)
          .jsonPath("status").isEqualTo("archived")
          .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
          .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
          .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
          .jsonPath("name").isEqualTo("new-premises-name")
      }
    }

    private fun assertTemporaryAccommodationPremisesBedSpaceIsArchived(premisesToGet: TemporaryAccommodationPremisesEntity) {
      val temporaryAccommodationPremise = temporaryAccommodationPremisesRepository.findById(premisesToGet.id)
      assertThat(temporaryAccommodationPremise).isNotNull()
      assertThat(temporaryAccommodationPremise.get().rooms).isNotEmpty()
      temporaryAccommodationPremise.get().rooms.forEach { room ->
        room.beds.forEach { bed ->
          assertThat(bed.endDate).isEqualTo(LocalDate.now())
        }
      }
    }
  }

  @Nested
  inner class GetPremisesById : IntegrationTestBase() {
    @Autowired
    lateinit var premisesTransformer: PremisesTransformer

    @Test
    fun `Get Premises by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = (0..5).map {
          givenATemporaryAccommodationPremises()
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 20))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns OK with correct body when capacity is used`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = (0..5).map {
          givenATemporaryAccommodationPremises()
        }.onEach {
          addRoomsAndBeds(it, roomCount = 4, 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises[2])
          withArrivalDate(LocalDate.now().minusDays(2))
          withDepartureDate(LocalDate.now().plusDays(4))
        }

        val premisesToGet = premises[2]
        val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20, 19))

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Premises by ID returns Not Found with correct body`() {
      val idToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/premises/$idToRequest")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
    }

    @Test
    fun `Get Temporary Accommodation Premises by ID for a premises not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val premisesToGet = premises[2]

        webTestClient.get()
          .uri("/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `The total bedspaces on a Temporary Accommodation Premises is equal to the sum of the bedspaces in all Rooms attached to the Premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }.also {
          addRoomsAndBeds(it, roomCount = 2, bedsPerRoom = 5)
          addRoomsAndBeds(it, roomCount = 1, bedsPerRoom = 1, isActive = false)
        }

        webTestClient.get()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.bedCount").isEqualTo(10)
      }
    }
  }

  @Nested
  inner class GetRoomsForPremises : IntegrationTestBase() {
    @Autowired
    lateinit var roomTransformer: RoomTransformer

    @Test
    fun `Get all Rooms for Premises returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get all Rooms for a Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get all Rooms for Premises returns OK with correct body with end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val rooms = roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }

        val bedEntityWithEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[0])
          withEndDate { LocalDate.now() }
        }

        val bedEntityWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(rooms[1])
        }

        rooms[0].beds += bedEntityWithEndDate
        rooms[1].beds += bedEntityWithoutEndDate

        val expectedJson = objectMapper.writeValueAsString(
          rooms.map {
            roomTransformer.transformJpaToApi(it)
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class CreateRoomForPremises : IntegrationTestBase() {
    @Test
    fun `Create new Room for Premises returns 201 Created with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `When a new room is created with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = null,
              name = "test-room",
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("notes").isEqualTo("")
      }
    }

    @Test
    fun `Create new Room with end date for temporary accommodation Premises returns 201 Created with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Trying to create a room without a name returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "",
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `Trying to create a room with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to create a room with a characteristic of the wrong service scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope("approved-premises")
        }.id

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `Trying to create a room with a characteristic of the wrong model scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope("temporary-accommodation")
        }.id

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }

    @Test
    fun `Create new Room for Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.post()
          .uri("/premises/${premises.id}/rooms")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewRoom(
              notes = "test notes",
              name = "test-room",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class UpdateRoom : IntegrationTestBase() {
    @Test
    fun `Updating a Room returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("test-room")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `When a room is updated with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = null,
              characteristicIds = mutableListOf(),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("notes").isEqualTo("")
      }
    }

    @Test
    fun `Trying to update a room that does not exist returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val id = UUID.randomUUID()

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/$id")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Room with an ID of $id could be found")
      }
    }

    @Test
    fun `Trying to update a room with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(UUID.randomUUID()),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Trying to update a room with a characteristic of the wrong service scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope("approved-premises")
        }.id

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `Trying to update a room with a characteristic of the wrong model scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope("temporary-accommodation")
        }.id

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = mutableListOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }

    @Test
    fun `Updating a Room on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room does not change the name when it's not provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("old-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room changes the name when it's provided`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedpace end-date when it's provided and no booking exists for the room`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking's arrival and departure date overlap with bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking starts in the future date compare to bedpace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.plusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking exists with departure date is equal to bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate)
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date throw conflict error when active booking exists with arrival date is equal to bedspace end-date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate)
          withDepartureDate(bedEndDate.plusDays(1))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("Conflict booking exists for the room with end date $bedEndDate: ${bookingEntity.id}")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists but ended before bedspace end-date `() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(3))
          withDepartureDate(bedEndDate.minusDays(1))
          withBed(bed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when active booking exists for different room`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val anotherRoom = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val anotherBed = bedEntityFactory.produceAndPersist {
          withRoom(anotherRoom)
        }
        anotherRoom.beds.add(anotherBed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(anotherBed)
        }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists for given bed but its been cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        val cancellationEntity = cancellationEntityFactory.produceAndPersist {
          withBooking(bookingEntity)
          withReason(cancellationReasonEntityFactory.produceAndPersist())
        }
        bookingEntity.cancellations += cancellationEntity

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace end-date is successful when booking exists but non-arrival`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        val bookingEntity = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withArrivalDate(bedEndDate.minusDays(1))
          withDepartureDate(bedEndDate.plusDays(2))
          withBed(bed)
        }

        val nonArrivalEntity = nonArrivalEntityFactory.produceAndPersist {
          withReason(nonArrivalReasonEntityFactory.produceAndPersist())
          withBooking(bookingEntity)
        }
        bookingEntity.nonArrival = nonArrivalEntity

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("name").isEqualTo("new-room-name")
          .jsonPath("notes").isEqualTo("test notes")
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { "temporary-accommodation" })
          .jsonPath("characteristics[*].name").isEqualTo(MutableList(5) { "Floor level access" })
          .jsonPath("beds[*].bedEndDate").isEqualTo(LocalDate.now().toString())
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedspace is not successful when end-date is already exists for the given bed`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate { LocalDate.now().plusDays(1) }
        }
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = LocalDate.now(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("bedEndDateCantBeModified")
      }
    }

    @Test
    fun `Updating a Temporary Accommodation room with bedpace end-date is before bed created date is throws bad request error`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val bedEndDate = LocalDate.now()
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("old-room-name")
        }
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withCreatedAt { bedEndDate.plusDays(5).toLocalDateTime() }
        }
        bed.apply { createdAt = bedEndDate.plusDays(1).toLocalDateTime() }
        bedRepository.save(bed)
        room.beds.add(bed)

        val characteristicIds = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope("temporary-accommodation")
          withName("Floor level access")
        }.map { it.id }

        webTestClient.put()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdateRoom(
              notes = "test notes",
              characteristicIds = characteristicIds,
              name = "new-room-name",
              bedEndDate = bedEndDate,
            ),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.detail")
          .isEqualTo("Bedspace end date cannot be prior to the Bedspace creation date: ${bed.createdAt!!.toLocalDate()}")
      }
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "provisional",
        "confirmed",
        "arrived",
      ],
    )
    fun `Archive Temporary Accommodation Premises with provisional, confirmed or arrived bookings throws bad request error`(bookingStatus: BookingStatus) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withService(ServiceName.temporaryAccommodation.value)
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withStatus(bookingStatus)
          withArrivalDate(LocalDate.now().plusDays(5))
        }

        webTestClient.put()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = premises.addressLine1,
              addressLine2 = premises.addressLine2,
              town = premises.town,
              postcode = premises.postcode,
              notes = "some arbitrary notes updated",
              localAuthorityAreaId = premises.localAuthorityArea?.id,
              probationRegionId = premises.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = premises.probationDeliveryUnit?.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.status")
          .jsonPath("invalid-params[0].errorType").isEqualTo("existingBookings")
      }
    }

    @Test
    fun `Archive Temporary Accommodation Premises with cancelled booking returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withService(ServiceName.temporaryAccommodation.value)
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
        }

        bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withPremises(premises)
          withStatus(BookingStatus.cancelled)
          withArrivalDate(LocalDate.now().plusDays(12))
        }

        webTestClient.put()
          .uri("/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            UpdatePremises(
              addressLine1 = premises.addressLine1,
              addressLine2 = premises.addressLine2,
              town = premises.town,
              postcode = premises.postcode,
              notes = "some arbitrary notes updated",
              localAuthorityAreaId = premises.localAuthorityArea?.id,
              probationRegionId = premises.probationRegion.id,
              characteristicIds = mutableListOf(),
              status = PropertyStatus.archived,
              pdu = null,
              probationDeliveryUnitId = premises.probationDeliveryUnit?.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("status").isEqualTo("archived")
      }
    }
  }

  @Nested
  inner class GetRoom : IntegrationTestBase() {
    @Autowired
    lateinit var roomTransformer: RoomTransformer

    @Test
    fun `Get Room by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = givenATemporaryAccommodationPremises()

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Room by ID returns Not Found with correct body when Premises does not exist`() {
      val premises = givenATemporaryAccommodationPremises()

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
        withName("test-room")
        withNotes("test notes")
      }

      val premisesIdToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/premises/$premisesIdToRequest/rooms/${room.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $premisesIdToRequest could be found")
    }

    @Test
    fun `Get Room by ID returns Not Found with correct body when Room does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, CAS3_REPORTER)) { _, jwt ->
        val premises = givenATemporaryAccommodationPremises()

        val roomIdToRequest = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/$roomIdToRequest")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Room with an ID of $roomIdToRequest could be found")
      }
    }

    @Test
    fun `Get Room by ID for a room on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get Room by ID for temporary accommodation returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }

    @Test
    fun `Get Room by ID for temporary accommodation returns OK with correct body with end date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { premises }
          withName("test-room")
          withNotes("test notes")
        }
        val bedEntity = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate { LocalDate.now() }
        }

        val expectedJson = objectMapper.writeValueAsString(roomTransformer.transformJpaToApi(bedEntity.room))

        webTestClient.get()
          .uri("/premises/${premises.id}/rooms/${room.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }
}

fun IntegrationTestBase.addRoomsAndBeds(premises: PremisesEntity, roomCount: Int, bedsPerRoom: Int, isActive: Boolean = true): List<RoomEntity> = roomEntityFactory.produceAndPersistMultiple(roomCount) {
  withYieldedPremises { premises }
}.onEach {
  bedEntityFactory.produceAndPersistMultiple(bedsPerRoom) {
    withYieldedRoom { it }
    if (!isActive) {
      withEndDate { LocalDate.now().minusDays(Random.nextLong(1, 10)) }
    }
  }
}.map {
  roomTestRepository.getReferenceById(it.id)
}
