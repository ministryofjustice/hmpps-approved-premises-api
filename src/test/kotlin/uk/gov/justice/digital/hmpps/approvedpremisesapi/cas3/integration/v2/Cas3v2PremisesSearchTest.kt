package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspacePremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3v2PremisesSearchTest : Cas3IntegrationTestBase() {

  private fun buildUri(params: Map<String, String>) = UriComponentsBuilder.fromPath("/cas3/v2/premises/search")
    .apply {
      if (params.isNotEmpty()) queryParams(LinkedMultiValueMap(params.mapValues { listOf(it.value) }))
    }
    .build()
    .toUriString()

  private fun doGetRequst(jwt: String, searchParameters: Map<String, String>) = webTestClient.get()
    .uri(buildUri(searchParameters))
    .headers(buildTemporaryAccommodationHeaders(jwt))
    .exchange()

  private fun createPremises(probationRegion: ProbationRegionEntity): List<Cas3PremisesEntity> {
    val premises = listOf(
      givenACas3Premises(probationRegion, Cas3PremisesStatus.archived),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.online),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.archived),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.online),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.archived),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.online),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.online),
      givenACas3Premises(probationRegion, Cas3PremisesStatus.online),
    )
    return premises
  }

  @Test
  fun `Searching for Premises returns OK with correct premises sorted and containing online, upcoming and archived bedspaces`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (_, expectedPremisesSearchResults) = getListPremises(user.probationRegion, null)
      val response = doGetRequst(jwt, emptyMap()).bodyAsObject<Cas3PremisesSearchResults>()
      assertThat(response).isEqualTo(expectedPremisesSearchResults)
    }
  }

  @Test
  fun `Searching for Premises when 'online' premises status is passed in to the query parameter returns only online premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (_, expectedPremisesSearchResults) = getListPremises(user.probationRegion, Cas3PremisesStatus.online)
      val response = doGetRequst(
        jwt,
        mapOf("premisesStatus" to Cas3PremisesStatus.online.value),
      ).bodyAsObject<Cas3PremisesSearchResults>()
      assertThat(response).isEqualTo(expectedPremisesSearchResults)
    }
  }

  @Test
  fun `Searching for Premises when 'archived' premises status is passed in to the query parameter returns only archived premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val (_, expectedPremisesSearchResults) = getListPremises(user.probationRegion, Cas3PremisesStatus.archived)
      val response = doGetRequst(
        jwt,
        mapOf("premisesStatus" to Cas3PremisesStatus.archived.value),
      ).bodyAsObject<Cas3PremisesSearchResults>()
      assertThat(response).isEqualTo(expectedPremisesSearchResults)
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
      val response = doGetRequst(jwt, emptyMap()).bodyAsObject<Cas3PremisesSearchResults>()
      assertThat(response).isEqualTo(expectedPremisesSearchResults)
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
        premisesStatus,
        postcodeToSearchBy,
        postcodeToSearchBy,
      )

      // filter premises with full postcode without whitespaces
      postcodeToSearchBy = premises.take(3).first().postcode

      assertPremisesFilteredByPostcode(
        jwt,
        premisesSearchResults,
        premisesStatus,
        postcodeToSearchBy,
        postcodeToSearchBy.replace(" ", ""),
      )

      // filter premises with partial postcode
      postcodeToSearchBy = premises.take(5).first().postcode

      assertPremisesFilteredByPostcode(
        jwt,
        premisesSearchResults,
        premisesStatus,
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
        premisesStatus,
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
        premisesStatus,
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
      val response = doGetRequst(
        jwt,
        mapOf("postcodeOrAddress" to randomStringMultiCaseWithNumbers(10)),
      ).bodyAsObject<Cas3PremisesSearchResults>()
      assertThat(response).isEqualTo(expectedPremisesSearchResults)
    }
  }

  private fun getPremises(probationRegion: ProbationRegionEntity) = cas3PremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withId(UUID.randomUUID())
    withAddressLine1(randomStringMultiCaseWithNumbers(20))
    withAddressLine2(randomStringLowerCase(10))
    withPostcode(randomPostCode())
    withStatus(Cas3PremisesStatus.online)
    withProbationDeliveryUnit(
      probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      },
    )
  }

  private fun getListPremises(
    probationRegion: ProbationRegionEntity,
    premisesStatus: Cas3PremisesStatus?,
  ): Pair<List<Cas3PremisesEntity>, Cas3PremisesSearchResults> {
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }
    val premisesSearchResult = mutableListOf<Cas3PremisesSearchResult>()
    val allPremises = mutableListOf<Cas3PremisesEntity>()

    val onlinePremisesWithBedspaceWithoutEndDate = V2().getListPremisesByStatus(
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 3,
      startDate = LocalDate.now().randomDateBefore(90),
      endDate = null,
      premisesStatus = Cas3PremisesStatus.online,
    ).map { premises ->
      val onlineBedspacesSearchResult =
        createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.online, true)
      val upcomingBedspacesSearchResult =
        createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.upcoming, true)
      if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.online) {
        premisesSearchResult.add(
          createPremisesSearchResult(premises, (onlineBedspacesSearchResult + upcomingBedspacesSearchResult)),
        )
      }
      premises
    }

    val onlinePremisesWithBedspaceWithEndDate = V2().getListPremisesByStatus(
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 3,
      premisesStatus = Cas3PremisesStatus.online,
      startDate = LocalDate.now().randomDateBefore(180),
      endDate = null,
    ).map { premises ->
      val onlineBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.online)
      val upcomingBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.upcoming)
      if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.online) {
        premisesSearchResult.add(
          createPremisesSearchResult(
            premises,
            (onlineBedspacesSearchResult + upcomingBedspacesSearchResult),
          ),
        )
      }
      premises
    }

    val archivedPremisesWithEndDateInTheFuture = V2().getListPremisesByStatus(
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 3,
      premisesStatus = Cas3PremisesStatus.archived,
      startDate = LocalDate.now().randomDateBefore(90),
      endDate = LocalDate.now().randomDateAfter(10),
    ).map { premises ->
      val archivedBedspacesInTheFutureSearchResult =
        createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.online, withEndDateInFuture = true)
      if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.online) {
        premisesSearchResult.add(createPremisesSearchResult(premises, (archivedBedspacesInTheFutureSearchResult)))
      }
      premises
    }

    val archivedPremises = V2().getListPremisesByStatus(
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 3,
      premisesStatus = Cas3PremisesStatus.archived,
      startDate = LocalDate.now().randomDateBefore(180),
      endDate = LocalDate.now().randomDateBefore(5),
    ).map { premises ->
      val archivedBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.archived)
      if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.archived) {
        premisesSearchResult.add(createPremisesSearchResult(premises, archivedBedspacesSearchResult))
      }
      premises
    }

    val archivedPremisesWithStartDateInTheFuture = V2().getListPremisesByStatus(
      probationDeliveryUnit = probationDeliveryUnit,
      localAuthorityArea = localAuthorityArea,
      numberOfPremises = 3,
      premisesStatus = Cas3PremisesStatus.online,
      startDate = LocalDate.now().randomDateAfter(15),
      endDate = null,
    ).map { premises ->
      val upcomingBedspacesSearchResult = createBedspacesAndBedspacesSearchResult(premises, Cas3BedspaceStatus.upcoming)
      if (premisesStatus == null || premisesStatus == Cas3PremisesStatus.archived) {
        premisesSearchResult.add(createPremisesSearchResult(premises, (upcomingBedspacesSearchResult)))
      }
      premises
    }

    allPremises.addAll(
      onlinePremisesWithBedspaceWithoutEndDate +
        onlinePremisesWithBedspaceWithEndDate +
        archivedPremises +
        archivedPremisesWithEndDateInTheFuture +
        archivedPremisesWithStartDateInTheFuture,
    )

    val premisesSearchResults = Cas3PremisesSearchResults(
      results = premisesSearchResult.sortedBy { it.id },
      totalPremises = premisesSearchResult.size,
      totalOnlineBedspaces = premisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.online }!! },
      totalUpcomingBedspaces = premisesSearchResult.sumOf { premises -> premises.bedspaces?.count { it.status == Cas3BedspaceStatus.upcoming }!! },
    )

    return Pair(allPremises.sortedBy { it.id }, premisesSearchResults)
  }

  private fun createBedspacesAndBedspacesSearchResult(
    premises: Cas3PremisesEntity,
    status: Cas3BedspaceStatus,
    withoutEndDate: Boolean = false,
    withEndDateInFuture: Boolean = false,
  ): List<Cas3BedspacePremisesSearchResult> {
    val bedspacesSearchResult = mutableListOf<Cas3BedspacePremisesSearchResult>()
    var startDate: LocalDate?
    var endDate: LocalDate?

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
          endDate = if (withEndDateInFuture) {
            LocalDate.now().randomDateAfter(30)
          } else {
            LocalDate.now().randomDateBefore(360)
          }
          startDate = endDate.randomDateBefore(360)
        }
      }

      val bedspace = V2().createBedspaceInPremises(premises, startDate, endDate)

      bedspacesSearchResult.add(createBedspaceSearchResult(bedspace.id, bedspace.reference, status))
    }

    return bedspacesSearchResult
  }

  private fun createBedspaceSearchResult(bedspaceId: UUID, name: String, bedspaceStatus: Cas3BedspaceStatus) = Cas3BedspacePremisesSearchResult(
    bedspaceId,
    name,
    bedspaceStatus,
  )

  private fun createPremisesSearchResult(
    premises: Cas3PremisesEntity,
    bedspaces: List<Cas3BedspacePremisesSearchResult>,
  ) = Cas3PremisesSearchResult(
    id = premises.id,
    reference = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    postcode = premises.postcode,
    town = premises.town,
    pdu = premises.probationDeliveryUnit.name,
    localAuthorityAreaName = premises.localAuthorityArea?.name!!,
    totalArchivedBedspaces = bedspaces.count { it.status == Cas3BedspaceStatus.archived },
    bedspaces = bedspaces,
  )

  private fun assertPremisesFilteredByPostcode(
    jwt: String,
    premisesSearchResults: Cas3PremisesSearchResults,
    premisesStatus: Cas3PremisesStatus,
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

    val response = doGetRequst(
      jwt,
      mapOf("premisesStatus" to premisesStatus.value, "postcodeOrAddress" to postcodeToSearchBy),
    ).bodyAsObject<Cas3PremisesSearchResults>()
    assertThat(response).isEqualTo(expectedPremisesSearchResults)
  }

  @SuppressWarnings("LongParameterList")
  private fun assertPremisesFilteredByAddress(
    jwt: String,
    premisesSearchResults: Cas3PremisesSearchResults,
    premisesStatus: Cas3PremisesStatus,
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
    val response = doGetRequst(
      jwt,
      mapOf("premisesStatus" to premisesStatus.value, "postcodeOrAddress" to addressToSearchBy),
    ).bodyAsObject<Cas3PremisesSearchResults>()
    assertThat(response).isEqualTo(expectedPremisesSearchResults)
  }
}
