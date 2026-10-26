package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient.Companion.MAX_CASE_ACCESS_REQUEST_COUNT
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ClientResultFailureArgumentsProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.uniqueRandomNumberChars

class LaoServiceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockLaoStrategy = mockk<LaoStrategy>()

  private val laoService = LaoService(mockApDeliusContextApiClient)

  private val username = "distinguished.name"

  @BeforeEach
  fun setupDefaultStrategy() {
    every { mockLaoStrategy.isNeverRestricted() } returns false
  }

  @Nested
  inner class IsVisibleForCaseSummaries {

    @BeforeEach
    fun setup() {
      every { mockLaoStrategy.deliusUsername } returns username
    }

    @Test
    fun `returns empty list and makes no calls when no case summaries provided`() {
      val result = laoService.isVisibleForCaseSummaries(emptyList(), mockLaoStrategy)

      assertThat(result).isEmpty()

      verify { mockApDeliusContextApiClient wasNot Called }
      verify { mockLaoStrategy wasNot Called }
    }

    @Test
    fun `returns visible without calling delius when strategy can view from the case summary alone`() {
      val caseSummary = CaseSummaryFactory().produce()

      every {
        mockLaoStrategy.canView(caseSummary)
      } returns LaoStrategy.CaseSummaryViewResult.CAN_VIEW

      val result = laoService.isVisibleForCaseSummaries(listOf(caseSummary), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          caseSummary.crn to true,
        ),
      )

      verify { mockApDeliusContextApiClient wasNot Called }
    }

    @Test
    fun `returns visible when case access is required and strategy allows the case access to be viewed`() {
      val caseSummary = CaseSummaryFactory().produce()
      val caseAccess = CaseAccessFactory().withCrn(caseSummary.crn).produce()

      every {
        mockLaoStrategy.canView(caseSummary)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(caseSummary.crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))
      every { mockLaoStrategy.canView(caseAccess) } returns true

      val result = laoService.isVisibleForCaseSummaries(listOf(caseSummary), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          caseSummary.crn to true,
        ),
      )
    }

    @Test
    fun `returns not visible when case access is required and strategy does not allow the case access to be viewed`() {
      val caseSummary = CaseSummaryFactory().produce()
      val caseAccess = CaseAccessFactory().withCrn(caseSummary.crn).produce()

      every {
        mockLaoStrategy.canView(caseSummary)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(caseSummary.crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))
      every { mockLaoStrategy.canView(caseAccess) } returns false

      val result = laoService.isVisibleForCaseSummaries(listOf(caseSummary), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          caseSummary.crn to false,
        ),
      )
    }

    @Test
    fun `returns a mix of visible and not visible when strategy gives mixed responses for the case access`() {
      val caseSummary1 = CaseSummaryFactory().produce()
      val caseSummary2 = CaseSummaryFactory().produce()
      val caseAccess1 = CaseAccessFactory().withCrn(caseSummary1.crn).produce()
      val caseAccess2 = CaseAccessFactory().withCrn(caseSummary2.crn).produce()

      every {
        mockLaoStrategy.canView(caseSummary1)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every {
        mockLaoStrategy.canView(caseSummary2)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(caseSummary1.crn, caseSummary2.crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess1, caseAccess2)))
      every { mockLaoStrategy.canView(caseAccess1) } returns true
      every { mockLaoStrategy.canView(caseAccess2) } returns false

      val result = laoService.isVisibleForCaseSummaries(listOf(caseSummary1, caseSummary2), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          caseSummary1.crn to true,
          caseSummary2.crn to false,
        ),
      )
    }

    @Test
    fun `only checks case access for crns where the case summary is indeterminate`() {
      val determinable = CaseSummaryFactory().produce()
      val indeterminate = CaseSummaryFactory().produce()
      val caseAccess = CaseAccessFactory().withCrn(indeterminate.crn).produce()

      every {
        mockLaoStrategy.canView(determinable)
      } returns LaoStrategy.CaseSummaryViewResult.CAN_VIEW
      every {
        mockLaoStrategy.canView(indeterminate)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(indeterminate.crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))
      every { mockLaoStrategy.canView(caseAccess) } returns false

      val result = laoService.isVisibleForCaseSummaries(listOf(determinable, indeterminate), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          determinable.crn to true,
          indeterminate.crn to false,
        ),
      )

      verify(exactly = 1) { mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(indeterminate.crn)) }
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when case access lookup fails`(response: ClientResult.Failure<UserAccess>) {
      val caseSummary = CaseSummaryFactory().produce()

      every {
        mockLaoStrategy.canView(caseSummary)
      } returns LaoStrategy.CaseSummaryViewResult.CHECK_CASE_ACCESS
      every { mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(caseSummary.crn)) } returns response

      val expectedException = response.toException()

      assertThatThrownBy {
        laoService.isVisibleForCaseSummaries(listOf(caseSummary), mockLaoStrategy)
      }
        .isInstanceOf(expectedException::class.java)
        .hasMessage(expectedException.message)
    }
  }

  @Nested
  inner class IsVisible {

    @Test
    fun `returns empty list and makes no calls when no crns provided`() {
      val result = laoService.isVisible(emptyList(), mockLaoStrategy)

      assertThat(result).isEmpty()

      verify { mockApDeliusContextApiClient wasNot Called }
      verify { mockLaoStrategy wasNot Called }
    }

    @Test
    fun `makes no upstream calls if lao strategy indicates to ignore exclusions and restrictions`() {
      val crn1 = uniqueRandomNumberChars(8)
      val crn2 = uniqueRandomNumberChars(8)
      val crn3 = uniqueRandomNumberChars(8)

      every { mockLaoStrategy.isNeverRestricted() } returns true

      val result = laoService.isVisible(listOf(crn1, crn2, crn3), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          crn1 to true,
          crn2 to true,
          crn3 to true,
        ),
      )
    }

    @Test
    fun `throws internal server error when more than the maximum number of crns are provided`() {
      val crns = (1..MAX_CASE_ACCESS_REQUEST_COUNT + 1).map { uniqueRandomNumberChars(8) }

      assertThatThrownBy {
        laoService.isVisible(crns, mockLaoStrategy)
      }
        .isInstanceOf(InternalServerErrorProblem::class.java)
        .hasMessage("Cannot request access details for more than $MAX_CASE_ACCESS_REQUEST_COUNT CRNs. ${crns.size} have been provided.")

      verify { mockApDeliusContextApiClient wasNot Called }
    }

    @Test
    fun `accepts exactly the maximum number of crns`() {
      val crns = (1..MAX_CASE_ACCESS_REQUEST_COUNT).map { uniqueRandomNumberChars(8) }
      val access = crns.map { CaseAccessFactory().withCrn(it).produce() }

      every { mockLaoStrategy.deliusUsername } returns username
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, crns)
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(access))
      every { mockLaoStrategy.canView(any<CaseAccess>()) } returns true

      val result = laoService.isVisible(crns, mockLaoStrategy)

      assertThat(result).hasSize(MAX_CASE_ACCESS_REQUEST_COUNT)
    }

    @Test
    fun `returns visible when lao strategy returns is visible`() {
      val crn = uniqueRandomNumberChars(8)
      val caseAccess = CaseAccessFactory().withCrn(crn).produce()

      every { mockLaoStrategy.deliusUsername } returns username
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))
      every { mockLaoStrategy.canView(caseAccess) } returns true

      val result = laoService.isVisible(listOf(crn), mockLaoStrategy)

      assertThat(result).isEqualTo(mapOf(crn to true))
    }

    @Test
    fun `returns not visible when lao strategy returns is not visible`() {
      val crn = uniqueRandomNumberChars(8)
      val caseAccess = CaseAccessFactory().withCrn(crn).produce()

      every { mockLaoStrategy.deliusUsername } returns username
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))
      every { mockLaoStrategy.canView(caseAccess) } returns false

      val result = laoService.isVisible(listOf(crn), mockLaoStrategy)

      assertThat(result).isEqualTo(mapOf(crn to false))
    }

    @Test
    fun `returns a mix of visible and not visible when strategy gives mixed responses for the case access`() {
      val crn1 = uniqueRandomNumberChars(8)
      val crn2 = uniqueRandomNumberChars(8)
      val crn3 = uniqueRandomNumberChars(8)
      val caseAccess1 = CaseAccessFactory().withCrn(crn1).produce()
      val caseAccess2 = CaseAccessFactory().withCrn(crn2).produce()
      val caseAccess3 = CaseAccessFactory().withCrn(crn3).produce()

      every { mockLaoStrategy.deliusUsername } returns username
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(crn1, crn2, crn3))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess1, caseAccess2, caseAccess3)))
      every { mockLaoStrategy.canView(caseAccess1) } returns true
      every { mockLaoStrategy.canView(caseAccess2) } returns false
      every { mockLaoStrategy.canView(caseAccess3) } returns true

      val result = laoService.isVisible(listOf(crn1, crn2, crn3), mockLaoStrategy)

      assertThat(result).isEqualTo(
        mapOf(
          crn1 to true,
          crn2 to false,
          crn3 to true,
        ),
      )
    }

    @Test
    fun `throws exception when response does not include access for a requested crn`() {
      val crn = uniqueRandomNumberChars(8)

      every { mockLaoStrategy.deliusUsername } returns username
      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(crn))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      assertThatThrownBy {
        laoService.isVisible(listOf(crn), mockLaoStrategy)
      }
        .isInstanceOf(IllegalStateException::class.java)
        .hasMessage("No case access found for CRN $crn")
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when client returns a failure`(response: ClientResult.Failure<UserAccess>) {
      val crn = uniqueRandomNumberChars(8)

      every { mockLaoStrategy.deliusUsername } returns username
      every { mockApDeliusContextApiClient.getUserAccessForCrns(username, listOf(crn)) } returns response

      val expectedException = response.toException()

      assertThatThrownBy {
        laoService.isVisible(listOf(crn), mockLaoStrategy)
      }
        .isInstanceOf(expectedException::class.java)
        .hasMessage(expectedException.message)
    }
  }
}
