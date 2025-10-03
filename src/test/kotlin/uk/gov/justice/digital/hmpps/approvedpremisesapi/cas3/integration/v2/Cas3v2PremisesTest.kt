package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import java.time.LocalDate
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.InvalidParam
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.failsWithValidationMessages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.withForbiddenMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.withNotFoundMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class Cas3v2PremisesTest : IntegrationTestBase() {

  @Autowired
  private lateinit var cas3PremisesTransformer: Cas3PremisesTransformer

  fun createCharacteristics(count: Int = 1): List<Cas3PremisesCharacteristicEntity> = cas3PremisesCharacteristicEntityFactory.produceAndPersistMultiple(count).sortedBy { it.name }

  fun createPdu(probationRegionEntity: ProbationRegionEntity) = probationDeliveryUnitFactory.produceAndPersist {
    withProbationRegion(probationRegionEntity)
  }

  @Nested
  inner class UpdatePremises {

    private fun doPutRequest(
      jwt: String,
      premisesId: UUID,
      cas3Premises: Cas3UpdatePremises,
    ) = webTestClient.put()
      .uri("/cas3/v2/premises/$premisesId")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .bodyValue(cas3Premises)
      .exchange()

    private fun buildUpdatedPremises(
      pdu: ProbationDeliveryUnitEntity,
      characteristicIds: List<UUID> = emptyList(),
      reference: String = randomStringMultiCaseWithNumbers(10),
    ) = Cas3UpdatePremises(
      reference = reference,
      addressLine1 = randomStringMultiCaseWithNumbers(25),
      addressLine2 = randomStringMultiCaseWithNumbers(10),
      postcode = randomPostCode(),
      town = randomNumberChars(10),
      notes = randomStringMultiCaseWithNumbers(100),
      probationRegionId = pdu.probationRegion.id,
      probationDeliveryUnitId = pdu.id,
      localAuthorityAreaId = null,
      characteristicIds = characteristicIds,
      turnaroundWorkingDayCount = 59,
    )

    @Test
    fun `Update premises returns 200 OK with correct body`() {
      givenAUser { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val newCharacteristic = cas3PremisesCharacteristicEntityFactory.produceAndPersist()
        val updatedPremises = buildUpdatedPremises(premises.probationDeliveryUnit, listOf(newCharacteristic.id))

        val result = doPutRequest(jwt, premises.id, updatedPremises)
          .expectStatus()
          .isOk()
          .bodyAsObject<Cas3Premises>()

        assertAll({
          assertThat(result.id).isEqualTo(premises.id)
          assertThat(result.reference).isEqualTo(updatedPremises.reference)
          assertThat(result.addressLine1).isEqualTo(updatedPremises.addressLine1)
          assertThat(result.addressLine2).isEqualTo(updatedPremises.addressLine2)
          assertThat(result.town).isEqualTo(updatedPremises.town)
          assertThat(result.postcode).isEqualTo(updatedPremises.postcode)
          assertThat(result.localAuthorityArea).isNull()
          assertThat(result.probationRegion.id).isEqualTo(premises.probationDeliveryUnit.probationRegion.id)
          assertThat(result.probationDeliveryUnit.id).isEqualTo(premises.probationDeliveryUnit.id)
          assertThat(result.status).isEqualTo(Cas3PremisesStatus.online)
          assertThat(result.totalOnlineBedspaces).isEqualTo(0)
          assertThat(result.totalUpcomingBedspaces).isEqualTo(0)
          assertThat(result.totalArchivedBedspaces).isEqualTo(0)
          assertThat(result.characteristics).isNull()
          assertThat(result.premisesCharacteristics).hasSize(1)
          assertThat(result.premisesCharacteristics!!.first().id).isEqualTo(newCharacteristic.id)
          assertThat(result.startDate).isEqualTo(premises.startDate)
          assertThat(result.endDate).isNull()
          assertThat(result.scheduleUnarchiveDate).isNull()
          assertThat(result.notes).isEqualTo(updatedPremises.notes)
          assertThat(result.turnaroundWorkingDays).isEqualTo(59)
          assertThat(result.archiveHistory).isEmpty()
        })
        val entity = cas3PremisesRepository.findById(premises.id).get()
        assertThat(result).isEqualTo(
          cas3PremisesTransformer.toCas3Premises(entity),
        )
      }
    }

    @Test
    fun `Update premises returns 403 Forbidden when user access is not allowed as they are out of region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (_, anotherJwt) = givenAUser(
          roles = listOf(UserRole.CAS3_ASSESSOR),
        )
        val premises = givenACas3Premises(user.probationRegion)
        val updatedPremises = buildUpdatedPremises(premises.probationDeliveryUnit)

        doPutRequest(anotherJwt, premises.id, updatedPremises)
          .expectStatus()
          .isForbidden
          .withForbiddenMessage()
      }
    }

    @Test
    fun `Update premises returns 404 when premises to update is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(user.probationRegion)
        val updatedPremises = buildUpdatedPremises(premises.probationDeliveryUnit)

        val id = UUID.randomUUID()

        doPutRequest(jwt, id, updatedPremises)
          .expectStatus()
          .isNotFound
          .withNotFoundMessage("No Cas3Premises with an ID of $id could be found")
      }
    }

    @Test
    fun `Update premises returns 400 error when premises name already used in probation region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(probationDeliveryUnit = user.probationDeliveryUnit!!)
        val anotherPremises = givenACas3Premises(probationDeliveryUnit = user.probationDeliveryUnit!!)
        val updatedPremises = buildUpdatedPremises(premises.probationDeliveryUnit, reference = anotherPremises.name)

        doPutRequest(jwt, premises.id, updatedPremises)
          .expectStatus()
          .is4xxClientError
          .failsWithValidationMessages(InvalidParam(propertyName = "$.reference", errorType = "notUnique"))
      }
    }
  }

  @Nested
  inner class CreatePremises {
    @Test
    fun `Create new premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val characteristics = createCharacteristics(5)
        val pdu = createPdu(user.probationRegion)

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = randomStringMultiCaseWithNumbers(12),
          town = randomStringMultiCaseWithNumbers(10),
          postcode = randomPostCode(),
          localAuthorityAreaId = localAuthorityArea.id,
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = characteristics.map { it.id },
          notes = randomStringLowerCase(100),
          turnaroundWorkingDays = 3,
        )

        val result = webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isCreated
          .bodyAsObject<Cas3Premises>()

        assertAll({
          assertThat(result.id).isNotNull
          assertThat(result.reference).isEqualTo(newPremises.reference)
          assertThat(result.addressLine1).isEqualTo(newPremises.addressLine1)
          assertThat(result.addressLine2).isEqualTo(newPremises.addressLine2)
          assertThat(result.town).isEqualTo(newPremises.town)
          assertThat(result.postcode).isEqualTo(newPremises.postcode)
          assertThat(result.localAuthorityArea!!.id).isEqualTo(localAuthorityArea.id)
          assertThat(result.probationRegion.id).isEqualTo(newPremises.probationRegionId)
          assertThat(result.probationDeliveryUnit.id).isEqualTo(newPremises.probationDeliveryUnitId)
          assertThat(result.notes).isEqualTo(newPremises.notes)
          assertThat(result.turnaroundWorkingDays).isEqualTo(newPremises.turnaroundWorkingDays)
          assertThat(result.characteristics).isNull()
          assertThat(result.premisesCharacteristics).hasSize(5)
          assertThat(result.premisesCharacteristics).containsAll(characteristics.map { it.toCas3PremisesCharacteristic() })
          assertThat(result.status).isEqualTo(Cas3PremisesStatus.online)
          assertThat(result.totalOnlineBedspaces).isEqualTo(0)
          assertThat(result.totalUpcomingBedspaces).isEqualTo(0)
          assertThat(result.totalArchivedBedspaces).isEqualTo(0)
          assertThat(result.startDate).isEqualTo(LocalDate.now())
          assertThat(result.endDate).isNull()
          assertThat(result.scheduleUnarchiveDate).isNull()
          assertThat(result.archiveHistory).isEmpty()
        })
      }
    }

    @Test
    fun `When a new premises is created with default values for optional properties returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val pdu = createPdu(user.probationRegion)

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = null,
          town = null,
          postcode = randomPostCode(),
          localAuthorityAreaId = null,
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = emptyList(),
          notes = null,
          turnaroundWorkingDays = null,
        )

        val result = webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isCreated
          .bodyAsObject<Cas3Premises>()

        assertAll({
          assertThat(result.id).isNotNull
          assertThat(result.reference).isEqualTo(newPremises.reference)
          assertThat(result.addressLine1).isEqualTo(newPremises.addressLine1)
          assertThat(result.addressLine2).isNull()
          assertThat(result.town).isNull()
          assertThat(result.postcode).isEqualTo(newPremises.postcode)
          assertThat(result.localAuthorityArea).isNull()
          assertThat(result.probationRegion.id).isEqualTo(newPremises.probationRegionId)
          assertThat(result.probationDeliveryUnit.id).isEqualTo(newPremises.probationDeliveryUnitId)
          assertThat(result.notes).isEmpty()
          assertThat(result.turnaroundWorkingDays).isEqualTo(2)
          assertThat(result.characteristics).isNull()
          assertThat(result.premisesCharacteristics).isEmpty()
          assertThat(result.status).isEqualTo(Cas3PremisesStatus.online)
          assertThat(result.totalOnlineBedspaces).isEqualTo(0)
          assertThat(result.totalUpcomingBedspaces).isEqualTo(0)
          assertThat(result.totalArchivedBedspaces).isEqualTo(0)
          assertThat(result.startDate).isEqualTo(LocalDate.now())
          assertThat(result.endDate).isNull()
          assertThat(result.scheduleUnarchiveDate).isNull()
          assertThat(result.archiveHistory).isEmpty()
        })
      }
    }

    @Test
    fun `Create new premises returns error when premises name already used in probation region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val characteristics = createCharacteristics(5)
        val pdu = createPdu(user.probationRegion)
        val (anotherUser, anotherJwt) = givenAUser(
          roles = listOf(UserRole.CAS3_ASSESSOR),
        )

        val reference = randomStringMultiCaseWithNumbers(25).uppercase()

        val newPremises = Cas3NewPremises(
          reference = reference,
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = randomStringMultiCaseWithNumbers(12),
          town = randomStringMultiCaseWithNumbers(10),
          postcode = randomPostCode(),
          localAuthorityAreaId = localAuthorityArea.id,
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = characteristics.map { it.id },
          notes = randomStringLowerCase(100),
          turnaroundWorkingDays = 3,
        )

        webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isCreated

        // fails with same name and pdu, ignores case of reference
        webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newPremises.copy(reference = reference.lowercase()))
          .exchange()
          .expectStatus()
          .is4xxClientError
          .failsWithValidationMessages(InvalidParam(propertyName = "$.reference", errorType = "notUnique"))

        // passes with same name but different pdu
        webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(anotherJwt))
          .bodyValue(
            newPremises.copy(
              probationRegionId = anotherUser.probationRegion.id,
              probationDeliveryUnitId = anotherUser.probationDeliveryUnit!!.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
      }
    }

    @Test
    fun `Create new Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val anotherProbationRegion = probationRegionEntityFactory.produceAndPersist()
        val pdu = createPdu(user.probationRegion)

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          postcode = randomPostCode(),
          probationRegionId = anotherProbationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }
}
