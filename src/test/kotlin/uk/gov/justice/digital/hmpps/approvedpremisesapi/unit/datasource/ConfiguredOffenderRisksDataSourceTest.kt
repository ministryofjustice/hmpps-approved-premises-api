package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.ConfiguredOffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSourceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks

class ConfiguredOffenderRisksDataSourceTest {
  private val mockCommunityApiDataSource = mockk<OffenderRisksDataSource>()
  private val mockApDeliusDataSource = mockk<OffenderRisksDataSource>()

  @BeforeEach
  fun setup() {
    every { mockCommunityApiDataSource.name } returns OffenderRisksDataSourceName.COMMUNITY_API
    every { mockApDeliusDataSource.name } returns OffenderRisksDataSourceName.AP_DELIUS_CONTEXT_API
  }

  @Test
  fun `getOffenderDetailSummary delegates to the Community API data source when configured`() {
    every { mockCommunityApiDataSource.getPersonRisks(any()) } returns mockk<PersonRisks>()

    val source = getConfiguredDataSource(OffenderRisksDataSourceName.COMMUNITY_API)

    source.getPersonRisks("FOO")

    verify(exactly = 1) {
      mockCommunityApiDataSource.getPersonRisks("FOO")
    }
    verify(exactly = 0) {
      mockApDeliusDataSource.getPersonRisks(any())
    }
  }

  @Test
  fun `getOffenderDetailSummary delegates to the AP-Delius data source when configured`() {
    every { mockApDeliusDataSource.getPersonRisks(any()) } returns mockk<PersonRisks>()

    val source = getConfiguredDataSource(OffenderRisksDataSourceName.AP_DELIUS_CONTEXT_API)

    source.getPersonRisks("FOO")

    verify(exactly = 0) {
      mockCommunityApiDataSource.getPersonRisks(any())
    }
    verify(exactly = 1) {
      mockApDeliusDataSource.getPersonRisks("FOO")
    }
  }

  private fun getConfiguredDataSource(sourceName: OffenderRisksDataSourceName): ConfiguredOffenderRisksDataSource {
    return ConfiguredOffenderRisksDataSource(
      dataSources = listOf(mockCommunityApiDataSource, mockApDeliusDataSource),
      dataSourceName = sourceName,
    )
  }
}
