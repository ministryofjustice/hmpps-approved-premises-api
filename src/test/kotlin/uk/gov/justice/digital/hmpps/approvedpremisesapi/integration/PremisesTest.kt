package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var premisesTransformer: PremisesTransformer

  @Autowired
  lateinit var staffMemberTransformer: StaffMemberTransformer

  @Autowired
  lateinit var roomTransformer: RoomTransformer

  @Test
  fun `Create new premises returns 201`() {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
  }

  @Test
  fun `When a new premises is created then all field data is persisted`() {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
            name = "some arbitrary name",
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
        .jsonPath("name").isEqualTo("some arbitrary name")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(5)
    }
  }

  @Test
  fun `When a new Temporary Accommodation premises is created with a legacy PDU name then all field data is persisted`() {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
            name = "some arbitrary name",
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
        .jsonPath("name").isEqualTo("some arbitrary name")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
    }
  }

  @Test
  fun `When an Approved Premises is updated then all field data is persisted`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
            probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"), // North West
            characteristicIds = mutableListOf(),
            status = PropertyStatus.archived,
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
        .jsonPath("probationRegion.id").isEqualTo("a02b7727-63aa-46f2-80f1-e0b05b31903c")
        .jsonPath("probationRegion.name").isEqualTo("North West")
        .jsonPath("status").isEqualTo("archived")
        .jsonPath("turnaroundWorkingDayCount").doesNotExist()
    }
  }

  @Test
  fun `When a Temporary Accommodation Premises is updated then all field data is persisted`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
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
    }
  }

  @Test
  fun `When a Temporary Accommodation Premises is updated with a legacy PDU name then all field data is persisted`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
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
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

      val otherProbationRegion = probationRegionEntityFactory.produceAndPersist {
        withId(UUID.randomUUID())
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(otherProbationRegion)
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
            probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"), // North West
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
  fun `Trying to create a new premises without a name returns 400`() {
    `Given a User` { user, jwt ->
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
  }

  @Test
  fun `Trying to update a premises with an invalid local authority area id returns 400`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("878217f0-6db5-49d8-a5a1-c40fdecd6060"), // not in db
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
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
  fun `Trying to update an Approved Premises with no local authority area id returns 400`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
      }

      val premisesToGet = premises[0]

      webTestClient.put()
        .uri("/premises/${premisesToGet.id}")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          UpdatePremises(
            addressLine1 = "1 somewhere updated",
            postcode = "AB456CD",
            notes = "some arbitrary notes updated",
            localAuthorityAreaId = null,
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
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
  }

  @Test
  fun `Trying to update a premises with an invalid probation region id returns 400`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"),
            probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"), // not in db
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
            probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"), // North West
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
            probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"), // North West
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

  @Test
  fun `Trying to update a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(1) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
            localAuthorityAreaId = UUID.fromString("d1bd139b-7b90-4aae-87aa-9f93e183a7ff"), // Allerdale
            probationRegionId = UUID.fromString("a02b7727-63aa-46f2-80f1-e0b05b31903c"), // North West
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
  fun `Trying to create a new premises with a non-unique name returns 400`() {
    `Given a User` { user, jwt ->
      temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
  }

  @Test
  fun `When a new premises is created with no notes then it defaults to empty`() {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
            name = "some arbitrary name",
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
  }

  @Test
  fun `Trying to create a new premises without an address returns 400`() {
    `Given a User` { user, jwt ->
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
  }

  @Test
  fun `Trying to create a new premises without a postcode returns 400`() {
    `Given a User` { user, jwt ->
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
  }

  @Test
  fun `Trying to create a new premises without a service returns 400`() {
    `Given a User` { _, jwt ->
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
  }

  @Test
  fun `Trying to create a new premises with an invalid local authority area id returns 400`() {
    `Given a User` { user, jwt ->
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
            localAuthorityAreaId = UUID.fromString("878217f0-6db5-49d8-a5a1-c40fdecd6060"), // not in db
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
  }

  @Test
  fun `Trying to create a new Approved Premises with no local authority area id returns 400`() {
    `Given a User` { _, jwt ->
      webTestClient.post()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .bodyValue(
          NewPremises(
            name = "arbitrary_test_name",
            addressLine1 = "1 somewhere",
            postcode = "AB456CD",
            notes = "some arbitrary notes",
            localAuthorityAreaId = null,
            probationRegionId = UUID.fromString("c5acff6c-d0d2-4b89-9f4d-89a15cfa3891"),
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
  }

  @Test
  fun `Trying to create a new Temporary Accommodation premises with a probation region that's not the user's region returns 403 Forbidden`() {
    `Given a User` { _, jwt ->
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
            probationRegionId = UUID.fromString("48f96076-e911-4419-bceb-95a3e7f417eb"), // not in db
            characteristicIds = mutableListOf(),
            status = PropertyStatus.active,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Trying to create a Temporary Accommodation Premises without a PDU returns 400`() {
    `Given a User` { user, jwt ->
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
            name = "some arbitrary name",
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
  }

  @Test
  fun `Trying to create a Temporary Accommodation Premises with an invalid probation delivery unit ID returns 400`() {
    `Given a User` { user, jwt ->
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
            name = "some arbitrary name",
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
  }

  @Test
  fun `Trying to create a Temporary Accommodation Premises with an invalid PDU name returns 400`() {
    `Given a User` { user, jwt ->
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
            name = "some arbitrary name",
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

  @Test
  fun `Get all Premises returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val cas1Premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withService("CAS1")
        withTotalBeds(20)
      }

      val cas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withService("CAS3")
        withTotalBeds(20)
      }

      val premises = cas1Premises + cas3Premises

      val expectedJson = objectMapper.writeValueAsString(
        premises.map {
          premisesTransformer.transformJpaToApi(it, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Premises for CAS1 returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withService("CAS1")
        withTotalBeds(20)
      }

      // Add some extra premises for the other service that shouldn't be returned
      temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withService("CAS3")
        withTotalBeds(20)
      }

      val expectedJson = objectMapper.writeValueAsString(
        premises.map {
          premisesTransformer.transformJpaToApi(it, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "approved-premises")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Premises for all regions in CAS3 returns 403 Forbidden`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get Premises in CAS3 for a region that's not the user's region returns 403 Forbidden`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .header("X-User-Region", UUID.randomUUID().toString())
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get Premises for a single region returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val regionId = UUID.randomUUID()

      val region = probationRegionEntityFactory.produceAndPersist {
        withId(regionId)
        withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      }

      val cas3Premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { region }
        withService("CAS3")
        withTotalBeds(20)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(region)
          }
        }
      }

      val cas1Premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { region }
        withService("CAS1")
        withTotalBeds(20)
      }

      // Add some extra premises in both services for other regions that shouldn't be returned
      val otherRegion = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist()
        }
      }

      temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { otherRegion }
        withService("CAS3")
        withTotalBeds(20)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(otherRegion)
          }
        }
      }

      approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { otherRegion }
        withService("CAS1")
        withTotalBeds(20)
      }

      val expectedPremises = cas1Premises + cas3Premises

      val expectedJson = objectMapper.writeValueAsString(
        expectedPremises.map {
          premisesTransformer.transformJpaToApi(it, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-User-Region", "$regionId")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Premises for a single region and particular service returns OK with correct body`() {
    `Given a User` { user, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withService("CAS3")
        withTotalBeds(20)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          }
        }
      }

      // Add some extra premises in the same region but in Approved Premises that shouldn't be returned
      approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
        withService("CAS1")
        withTotalBeds(20)
      }

      // Add some extra premises in both services for other regions that shouldn't be returned
      val otherProbationRegion = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          apAreaEntityFactory.produceAndPersist()
        }
      }

      temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(otherProbationRegion)
        withService("CAS3")
        withTotalBeds(20)
        withYieldedProbationDeliveryUnit {
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(otherProbationRegion)
          }
        }
      }

      approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(otherProbationRegion)
        withService("CAS1")
        withTotalBeds(20)
      }

      val expectedJson = objectMapper.writeValueAsString(
        premises.map {
          premisesTransformer.transformJpaToApi(it, 20)
        },
      )

      webTestClient.get()
        .uri("/premises")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", "temporary-accommodation")
        .header("X-User-Region", "${user.probationRegion.id}")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get Premises by ID returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
      }

      val premisesToGet = premises[2]
      val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20))

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
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
      }

      val keyWorker = ContextStaffMemberFactory().produce()
      premises.forEach {
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, it.qCode)
      }

      bookingEntityFactory.produceAndPersist {
        withPremises(premises[2])
        withArrivalDate(LocalDate.now().minusDays(2))
        withDepartureDate(LocalDate.now().plusDays(4))
        withStaffKeyWorkerCode(keyWorker.code)
      }

      val premisesToGet = premises[2]
      val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 19))

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
    `Given a User` { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
  fun `Get Premises Staff without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/staff")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Get Approved Premises Staff where delius team cannot be found returns 500 when use has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val qCode = "NOTFOUND"

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withQCode(qCode)
      }

      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/secure/teams/$qCode/staff"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(404),
          ),
      )

      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("$.detail").isEqualTo("No team found for QCode: ${premises.qCode}")
    }
  }

  @Test
  fun `Get Premises Staff for Temporary Accommodation Premises returns 501`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withProbationRegion(user.probationRegion)
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
    }
  }

  @Test
  fun `Get Premises Staff for Temporary Accommodation Premises not in the user's region returns 403 Forbidden`() {
    `Given a User` { user, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea {
              apAreaEntityFactory.produceAndPersist()
            }
          }
        }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  fun `Get Approved Premises Staff for Approved Premises returns 200 with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val qCode = "FOUND"

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withQCode(qCode)
      }

      val staffMembers = listOf(
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
      )

      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/approved-premises/$qCode/staff"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody(
                objectMapper.writeValueAsString(
                  StaffMembersPage(
                    content = staffMembers,
                  ),
                ),
              ),
          ),
      )

      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            staffMembers.map(staffMemberTransformer::transformDomainToApi),
          ),
        )
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "CAS1_MANAGER", "CAS1_MATCHER" ])
  @Disabled
  fun `Get Approved Premises Staff caches response when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val qCode = "FOUND"

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withQCode(qCode)
      }

      val staffMembers = listOf(
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
        ContextStaffMemberFactory().produce(),
      )

      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/approved-premises/$qCode/staff"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody(
                objectMapper.writeValueAsString(
                  StaffMembersPage(
                    content = staffMembers,
                  ),
                ),
              ),
          ),
      )

      repeat(2) {
        webTestClient.get()
          .uri("/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              staffMembers.map(staffMemberTransformer::transformDomainToApi),
            ),
          )
      }

      wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/approved-premises/$qCode/staff")))
    }
  }

  @Test
  fun `Get all Rooms for Premises returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist() {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User` { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
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
  fun `The total bedspaces on a Temporary Accommodation Premises is equal to the sum of the bedspaces in all Rooms attached to the Premises`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist() {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
      }

      bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom { room }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bedCount").isEqualTo(5)
    }
  }

  @Test
  fun `Create new Room for Premises returns 201 Created with correct body when given valid data`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
  fun `Trying to create a room without a name returns 400`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val characteristicId = characteristicEntityFactory.produceAndPersist {
        withModelScope("premises")
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
        .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
    }
  }

  @Test
  fun `Create new Room for Temporary Accommodation Premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
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

  @Test
  fun `Updating a Room returns OK with correct body when given valid data`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises }
        withName("test-room")
      }

      val characteristicId = characteristicEntityFactory.produceAndPersist {
        withModelScope("premises")
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
        .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
    }
  }

  @Test
  fun `Updating a Room on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
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
  fun `Get Room by ID returns OK with correct body`() {
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
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
  fun `Get Room by ID returns Not Found with correct body when Premises does not exist`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

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
    `Given a User` { _, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
        withTotalBeds(20)
      }

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
    `Given a User` { _, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withId(UUID.randomUUID())
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
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
  fun `When a new Temporary Accommodation premises is created with no turnaround working day count then it defaults to 2`() {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
            name = "some arbitrary name",
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
        .jsonPath("name").isEqualTo("some arbitrary name")
        .jsonPath("localAuthorityArea.id").isEqualTo("a5f52443-6b55-498c-a697-7c6fad70cc3f")
        .jsonPath("probationRegion.id").isEqualTo(user.probationRegion.id.toString())
        .jsonPath("status").isEqualTo("pending")
        .jsonPath("pdu").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("probationDeliveryUnit.id").isEqualTo(probationDeliveryUnit.id.toString())
        .jsonPath("probationDeliveryUnit.name").isEqualTo(probationDeliveryUnit.name)
        .jsonPath("turnaroundWorkingDayCount").isEqualTo(2)
    }
  }

  @ParameterizedTest(name = "Trying to create a new Temporary Accommodation premises with turnaround working day count = {0} returns 400 and errorType = {1}")
  @MethodSource("turnaroundWorkingDayCountProvider")
  fun `Trying to create a new Temporary Accommodation premises with turnaround working day count less than 1 returns 400`(
    turnaroundWorkingDayCount: Int,
    expectedErrorType: String,
  ) {
    `Given a User` { user, jwt ->
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(user.probationRegion)
      }

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
            name = "some arbitrary name",
            localAuthorityAreaId = UUID.fromString("a5f52443-6b55-498c-a697-7c6fad70cc3f"),
            probationRegionId = user.probationRegion.id,
            characteristicIds = mutableListOf(),
            status = PropertyStatus.pending,
            probationDeliveryUnitId = probationDeliveryUnit.id,
            turnaroundWorkingDayCount = 0,
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
  private companion object {
    @JvmStatic
    fun turnaroundWorkingDayCountProvider() = Stream.of(
      Arguments.of(0, "isNotAPositiveInteger"),
      Arguments.of(-4, "isNotAPositiveInteger"),
    )
  }
}
