package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.ConfiguredOffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSourceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

class ConfiguredOffenderDetailsDataSourceTest {
  private val mockCommunityApiDataSource = mockk<OffenderDetailsDataSource>()
  private val mockApDeliusDataSource = mockk<OffenderDetailsDataSource>()

  @BeforeEach
  fun setup() {
    every { mockCommunityApiDataSource.name } returns OffenderDetailsDataSourceName.COMMUNITY_API
    every { mockApDeliusDataSource.name } returns OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API
  }

  @Test
  fun `getOffenderDetailSummary delegates to the Community API data source when configured`() {
    every { mockCommunityApiDataSource.getOffenderDetailSummary(any()) } returns mockk<ClientResult<OffenderDetailSummary>>()
    every { mockCommunityApiDataSource.getOffenderDetailSummaries(any()) } returns mockk<Map<String, ClientResult<OffenderDetailSummary>>>()
    every { mockCommunityApiDataSource.getUserAccessForOffenderCrn(any(), any()) } returns mockk<ClientResult<UserOffenderAccess>>()
    every { mockCommunityApiDataSource.getUserAccessForOffenderCrns(any(), any()) } returns mockk<Map<String, ClientResult<UserOffenderAccess>>>()

    val source = getConfiguredDataSource(OffenderDetailsDataSourceName.COMMUNITY_API)

    source.getOffenderDetailSummary("FOO")
    source.getOffenderDetailSummaries(listOf("BAR", "BAZ"))
    source.getUserAccessForOffenderCrn("a-user", "FOO")
    source.getUserAccessForOffenderCrns("a-user", listOf("BAR", "BAZ"))

    verify(exactly = 1) {
      mockCommunityApiDataSource.getOffenderDetailSummary("FOO")
      mockCommunityApiDataSource.getOffenderDetailSummaries(listOf("BAR", "BAZ"))
      mockCommunityApiDataSource.getUserAccessForOffenderCrn("a-user", "FOO")
      mockCommunityApiDataSource.getUserAccessForOffenderCrns("a-user", listOf("BAR", "BAZ"))
    }
    verify(exactly = 0) {
      mockApDeliusDataSource.getOffenderDetailSummary(any())
      mockApDeliusDataSource.getOffenderDetailSummaries(any())
      mockApDeliusDataSource.getUserAccessForOffenderCrn(any(), any())
      mockApDeliusDataSource.getUserAccessForOffenderCrns(any(), any())
    }
  }

  @Test
  fun `getOffenderDetailSummary delegates to the AP-Delius data source when configured`() {
    every { mockApDeliusDataSource.getOffenderDetailSummary(any()) } returns mockk<ClientResult<OffenderDetailSummary>>()
    every { mockApDeliusDataSource.getOffenderDetailSummaries(any()) } returns mockk<Map<String, ClientResult<OffenderDetailSummary>>>()
    every { mockApDeliusDataSource.getUserAccessForOffenderCrn(any(), any()) } returns mockk<ClientResult<UserOffenderAccess>>()
    every { mockApDeliusDataSource.getUserAccessForOffenderCrns(any(), any()) } returns mockk<Map<String, ClientResult<UserOffenderAccess>>>()

    val source = getConfiguredDataSource(OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API)

    source.getOffenderDetailSummary("FOO")
    source.getOffenderDetailSummaries(listOf("BAR", "BAZ"))
    source.getUserAccessForOffenderCrn("a-user", "FOO")
    source.getUserAccessForOffenderCrns("a-user", listOf("BAR", "BAZ"))

    verify(exactly = 0) {
      mockCommunityApiDataSource.getOffenderDetailSummary(any())
      mockCommunityApiDataSource.getOffenderDetailSummaries(any())
      mockCommunityApiDataSource.getUserAccessForOffenderCrn(any(), any())
      mockCommunityApiDataSource.getUserAccessForOffenderCrns(any(), any())
    }
    verify(exactly = 1) {
      mockApDeliusDataSource.getOffenderDetailSummary("FOO")
      mockApDeliusDataSource.getOffenderDetailSummaries(listOf("BAR", "BAZ"))
      mockApDeliusDataSource.getUserAccessForOffenderCrn("a-user", "FOO")
      mockApDeliusDataSource.getUserAccessForOffenderCrns("a-user", listOf("BAR", "BAZ"))
    }
  }

  private fun getConfiguredDataSource(sourceName: OffenderDetailsDataSourceName): ConfiguredOffenderDetailsDataSource {
    return ConfiguredOffenderDetailsDataSource(
      dataSources = listOf(mockCommunityApiDataSource, mockApDeliusDataSource),
      dataSourceName = sourceName,
    )
  }
}
