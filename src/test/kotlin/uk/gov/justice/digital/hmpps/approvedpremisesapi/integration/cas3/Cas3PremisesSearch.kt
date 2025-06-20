package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspacePremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesSearch : Cas3IntegrationTestBase() {
  companion object Constants {
    const val PREMISES_SEARCH_API_URL = "/cas3/premises/search"
  }

  @Test
  fun `Searching for Premises returns OK with correct premises sorted and containing online, upcoming and archived bedspaces`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (_, expectedPremisesSearchResults) = getListPremises(user.probationRegion, null)

      assertUrlReturnsPremises(
        jwt,
        PREMISES_SEARCH_API_URL,
        expectedPremisesSearchResults,
      )
    }
  }

  @Test
  fun `Searching for Premises when 'online' premises status is passed in to the query parameter returns only online premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (premises, expectedPremisesSearchResults) = getListPremises(user.probationRegion, Cas3PremisesStatus.online)

      assertUrlReturnsPremises(
        jwt,
        "${PREMISES_SEARCH_API_URL}?propertyStatus=${Cas3PremisesStatus.online}",
        expectedPremisesSearchResults,
      )
    }
  }

  @Test
  fun `Searching for Premises when 'archived' premises status is passed in to the query parameter returns only archived premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (premises, expectedPremisesSearchResults) = getListPremises(user.probationRegion, Cas3PremisesStatus.archived)

      assertUrlReturnsPremises(
        jwt,
        "${PREMISES_SEARCH_API_URL}?propertyStatus=${Cas3PremisesStatus.archived}",
        expectedPremisesSearchResults,
      )
    }
  }

  @Test
  fun `Searching for Premises returns premises without bedspaces`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val premises = getPremises(user.probationRegion)
      val expectedPremisesSearchResult = createPremisesSearchResult(premises, listOf())
      val expectedPremisesSearchResults = Cas3PremisesSearchResults(
        results = listOf(expectedPremisesSearchResult),
        totalPremises = 1,
        totalOnlineBedspaces = 0,
        totalUpcomingBedspaces = 0,
      )

      assertUrlReturnsPremises(
        jwt,
        PREMISES_SEARCH_API_URL,
        expectedPremisesSearchResults,
      )
    }
  }

  @ParameterizedTest
  @EnumSource(value = Cas3PremisesStatus::class)
  fun `Searching for Premises when a postcode is passed in the query parameter returns only matched premises`(premisesStatus: Cas3PremisesStatus) {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (premises, premisesSearchResults) = getListPremises(user.probationRegion, premisesStatus)

      // filter premises with full postcode
      var postcodeToSearchBy = premises.take(2).first().postcode

      assertPremisesFilteredByPostcode(
        jwt,
        premisesSearchResults,
        postcodeToSearchBy,
        postcodeToSearchBy,
      )

      // filter premises with full postcode without whitespaces
      postcodeToSearchBy = premises.take(3).first().postcode

      assertPremisesFilteredByPostcode(
        jwt,
        premisesSearchResults,
        postcodeToSearchBy,
        postcodeToSearchBy.replace(" ", ""),
      )

      // filter premises with partial postcode
      postcodeToSearchBy = premises.take(5).first().postcode

      assertPremisesFilteredByPostcode(
        jwt,
        premisesSearchResults,
        postcodeToSearchBy,
        postcodeToSearchBy.split(" ").first(),
      )
    }
  }

  @ParameterizedTest
  @CsvSource(
    "addressLine1,online",
    "addressLine2,online",
    "addressLine1,archived",
    "addressLine2,archived",
  )
  fun `Searching for Premises when a premises address is passed in the query parameter returns only matched premises`(addressLineField: String, premisesStatus: Cas3PremisesStatus) {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (premises, premisesSearchResults) = getListPremises(user.probationRegion, premisesStatus)

      // filter premises with the full premises address
      var addressToSearchBy = when (addressLineField) {
        "addressLine1" -> premises.take(6).first().addressLine1
        "addressLine2" -> premises.take(6).first().addressLine2!!
        else -> error("unexpected value $addressLineField")
      }

      assertPremisesFilteredByAddress(
        jwt,
        premisesSearchResults,
        addressLineField,
        addressToSearchBy,
        addressToSearchBy,
      )

      // filter premises with the partial premises address
      addressToSearchBy = when (addressLineField) {
        "addressLine1" -> premises.take(2).first().addressLine1
        "addressLine2" -> premises.take(2).first().addressLine2!!
        else -> error("unexpected value $addressLineField")
      }

      assertPremisesFilteredByAddress(
        jwt,
        premisesSearchResults,
        addressLineField,
        addressToSearchBy,
        addressToSearchBy.split(" ").last(),
      )
    }
  }

  @Test
  fun `Searching for Premises returns successfully with no premises when a postcode or address is passed in the query parameter and doesn't match any premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val expectedPremisesSearchResults = Cas3PremisesSearchResults(
        results = emptyList(),
        totalPremises = 0,
        totalOnlineBedspaces = 0,
        totalUpcomingBedspaces = 0,
      )

      assertUrlReturnsPremises(
        jwt,
        "${PREMISES_SEARCH_API_URL}?postcodeOrAddress=${randomStringMultiCaseWithNumbers(10)}",
        expectedPremisesSearchResults,
      )
    }
  }

  private fun getPremises(probationRegion: ProbationRegionEntity) = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withProbationRegion(probationRegion)
    withId(UUID.randomUUID())
    withAddressLine1(randomStringMultiCaseWithNumbers(20))
    withAddressLine2(randomStringLowerCase(10))
    withPostcode(randomPostCode())
    withStatus(PropertyStatus.active)
    withYieldedProbationDeliveryUnit {
      probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
    }
    withService("CAS3")
  }

  private fun getListPremises(probationRegion: ProbationRegionEntity, premisesStatus: Cas3PremisesStatus?): Pair<List<TemporaryAccommodationPremisesEntity>, Cas3PremisesSearchResults> {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    val premisesSearchResult = mutableListOf<Cas3PremisesSearchResult>()
    val allPremises = mutableListOf<TemporaryAccommodationPremisesEntity>()

    if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.online) {
      val onlinePremisesWithBedspaceWithoutEndDate = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 5,
        propertyStatus = PropertyStatus.active,
      ).map { premises ->
        val onlineBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.online, true)
        val upcomingBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.upcoming, true)
        premisesSearchResult.add(createPremisesSearchResult(premises, (onlineBedspacesSearchResult + upcomingBedspacesSearchResult)))
        premises
      }

      allPremises.addAll(onlinePremisesWithBedspaceWithoutEndDate)

      val onlinePremisesWithBedspaceWithEndDate = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 5,
        propertyStatus = PropertyStatus.active,
      ).map { premises ->
        val onlineBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.online)
        val upcomingBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.upcoming)
        premisesSearchResult.add(createPremisesSearchResult(premises, (onlineBedspacesSearchResult + upcomingBedspacesSearchResult)))
        premises
      }

      allPremises.addAll(onlinePremisesWithBedspaceWithEndDate)
    }

    if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.archived) {
      val archivedPremises = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 3,
        propertyStatus = PropertyStatus.archived,
      ).map { premises ->
        val archivedBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.archived)
        premisesSearchResult.add(createPremisesSearchResult(premises, archivedBedspacesSearchResult))
        premises
      }

      allPremises.addAll(archivedPremises)
    }

    val premisesSearchResults = Cas3PremisesSearchResults(
      results = premisesSearchResult.sortedBy { it.id },
      totalPremises = premisesSearchResult.size,
      totalOnlineBedspaces = premisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.online }!! },
      totalUpcomingBedspaces = premisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.upcoming }!! },
    )

    return Pair(allPremises.sortedBy { it.id }, premisesSearchResults)
  }

  private fun createBedspacesAndBedspacesSearchResult(premises: TemporaryAccommodationPremisesEntity, status: Cas3BedspaceStatus, withoutEndDate: Boolean = false): List<Cas3BedspacePremisesSearchResult> {
    val bedspacesSearchResult = mutableListOf<Cas3BedspacePremisesSearchResult>()
    var startDate = LocalDate.now().minusDays(30)
    var endDate: LocalDate? = null

    repeat(randomInt(1, 5)) {
      when (status) {
        Cas3BedspaceStatus.online -> {
          startDate = LocalDate.now().randomDateBefore(360)
          endDate = when {
            withoutEndDate -> null
            else -> LocalDate.now().plusDays(1).randomDateAfter(90)
          }
        }
        Cas3BedspaceStatus.upcoming -> {
          startDate = LocalDate.now().plusDays(1).randomDateAfter(30)
          endDate = when {
            withoutEndDate -> null
            else -> startDate.plusDays(1).randomDateAfter(90)
          }
        }
        Cas3BedspaceStatus.archived -> {
          endDate = LocalDate.now().minusDays(1).randomDateBefore(360)
          startDate = endDate!!.randomDateBefore(360)
        }
      }

      val bedspace = createBedspaceInPremises(premises, startDate, endDate)

      bedspacesSearchResult.add(createBedspaceSearchResult(bedspace.id, bedspace.room.name, status))
    }

    return bedspacesSearchResult
  }

  private fun createBedspaceSearchResult(bedspaceId: UUID, name: String, bedspaceStatus: Cas3BedspaceStatus) = Cas3BedspacePremisesSearchResult(
    bedspaceId,
    name,
    bedspaceStatus,
  )

  private fun createPremisesSearchResult(premises: TemporaryAccommodationPremisesEntity, bedspaces: List<Cas3BedspacePremisesSearchResult>) = Cas3PremisesSearchResult(
    id = premises.id,
    reference = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    postcode = premises.postcode,
    town = premises.town,
    pdu = premises.probationDeliveryUnit?.name!!,
    localAuthorityAreaName = premises.localAuthorityArea?.name!!,
    totalArchivedBedspaces = bedspaces.count { it.status == Cas3BedspaceStatus.archived },
    bedspaces = bedspaces,
  )

  private fun assertPremisesFilteredByPostcode(
    jwt: String,
    premisesSearchResults: Cas3PremisesSearchResults,
    premisesPostcode: String,
    postcodeToSearchBy: String,
  ) {
    val expectedPremisesSearchResult = premisesSearchResults.results?.filter { it.postcode == premisesPostcode }
    val expectedPremisesSearchResults = Cas3PremisesSearchResults(
      results = expectedPremisesSearchResult ?: emptyList(),
      totalPremises = expectedPremisesSearchResult?.size!!,
      totalOnlineBedspaces = expectedPremisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.online }!! },
      totalUpcomingBedspaces = expectedPremisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.upcoming }!! },
    )

    assertUrlReturnsPremises(
      jwt,
      "$PREMISES_SEARCH_API_URL?postcodeOrAddress=$postcodeToSearchBy",
      expectedPremisesSearchResults,
    )
  }

  private fun assertPremisesFilteredByAddress(
    jwt: String,
    premisesSearchResults: Cas3PremisesSearchResults,
    addressLineField: String,
    premisesAddress: String,
    addressToSearchBy: String,
  ) {
    val expectedPremisesSearchResult = when (addressLineField) {
      "addressLine1" -> premisesSearchResults.results?.filter { it.addressLine1 == premisesAddress }
      "addressLine2" -> premisesSearchResults.results?.filter { it.addressLine2 == premisesAddress }
      else -> error("unexpected value $addressLineField")
    }

    val expectedPremisesSearchResults = Cas3PremisesSearchResults(
      results = expectedPremisesSearchResult ?: emptyList(),
      totalPremises = expectedPremisesSearchResult?.size!!,
      totalOnlineBedspaces = expectedPremisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.online }!! },
      totalUpcomingBedspaces = expectedPremisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.upcoming }!! },
    )

    assertUrlReturnsPremises(
      jwt,
      "$PREMISES_SEARCH_API_URL?postcodeOrAddress=$addressToSearchBy",
      expectedPremisesSearchResults,
    )
  }

  private fun assertUrlReturnsPremises(
    jwt: String,
    url: String,
    expectedPremisesSearchResults: Cas3PremisesSearchResults,
  ): WebTestClient.ResponseSpec {
    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk

    val responseBody = response
      .returnResult<String>()
      .responseBody
      .blockFirst()

    assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedPremisesSearchResults))

    return response
  }
}
