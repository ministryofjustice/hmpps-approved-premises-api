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

    val source = getConfiguredDataSource(OffenderDetailsDataSourceName.COMMUNITY_API)

    source.getOffenderDetailSummary("FOO")

    verify(exactly = 1) {
      mockCommunityApiDataSource.getOffenderDetailSummary("FOO")
    }
    verify(exactly = 0) {
      mockApDeliusDataSource.getOffenderDetailSummary(any())
    }
  }

  @Test
  fun `getOffenderDetailSummary delegates to the AP-Delius data source when configured`() {
    every { mockApDeliusDataSource.getOffenderDetailSummary(any()) } returns mockk<ClientResult<OffenderDetailSummary>>()

    val source = getConfiguredDataSource(OffenderDetailsDataSourceName.AP_DELIUS_CONTEXT_API)

    source.getOffenderDetailSummary("FOO")

    verify(exactly = 0) {
      mockCommunityApiDataSource.getOffenderDetailSummary(any())
    }
    verify(exactly = 1) {
      mockApDeliusDataSource.getOffenderDetailSummary("FOO")
    }
  }

  private fun getConfiguredDataSource(sourceName: OffenderDetailsDataSourceName): ConfiguredOffenderDetailsDataSource {
    return ConfiguredOffenderDetailsDataSource(
      dataSources = listOf(mockCommunityApiDataSource, mockApDeliusDataSource),
      dataSourceName = sourceName,
    )
  }
}
