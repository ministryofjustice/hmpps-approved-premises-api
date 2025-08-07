package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult

class StaffMemberServiceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockSentryService = mockk<SentryService>()
  private val staffMemberService = StaffMemberService(mockApDeliusContextApiClient, mockSentryService)

  private val qCode = "Q123"

  @Nested
  inner class GetStaffMemberByCode {
    @Test
    fun `it returns a staff member`() {
      val staffMembers = ContextStaffMemberFactory().produceMany().take(5).toList()

      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = StaffMembersPage(
          content = staffMembers,
        ),
      )

      val result = staffMemberService.getStaffMemberByCodeForPremise(staffMembers[2].code, qCode)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(staffMembers[2])
      }
    }

    @Test
    fun `it returns Unauthorised when Delius returns Unauthorised`() {
      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/staff-members/code",
        HttpStatus.UNAUTHORIZED,
        body = null,
      )

      val result = staffMemberService.getStaffMemberByCodeForPremise("code", qCode)

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `it returns NotFound when Delius returns NotFound`() {
      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/staff-members/code",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      every { mockSentryService.captureErrorMessage(any()) } returns Unit

      val result = staffMemberService.getStaffMemberByCodeForPremise("code", qCode)

      assertThatCasResult(result).isNotFound("Team", qCode)

      verify { mockSentryService.captureErrorMessage("404 returned when finding staff members for qcode 'Q123'") }
    }

    @Test
    fun `it returns NotFound when a staff member for the QCode cannot be found in the results`() {
      val staffMembers = ContextStaffMemberFactory().produceMany().take(5).toList()

      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = StaffMembersPage(
          content = staffMembers,
        ),
      )

      val result = staffMemberService.getStaffMemberByCodeForPremise("code", qCode)

      assertThatCasResult(result).isNotFound("Staff Code", "code")
    }
  }

  @Nested
  inner class GetStaffMembersForQCode {

    @Test
    fun success() {
      val staffMembers = ContextStaffMemberFactory().produceMany().take(5).toList()

      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = StaffMembersPage(
          content = staffMembers,
        ),
      )

      val result = staffMemberService.getStaffMembersForQCode(qCode)

      assertThatCasResult(result).isSuccess().with {
        assertThat(it.content).hasSize(5)
      }
    }

    @Test
    fun notFound() {
      every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/staff-members/code",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      every { mockSentryService.captureErrorMessage(any()) } returns Unit

      val result = staffMemberService.getStaffMembersForQCode(qCode)

      assertThatCasResult(result).isNotFound("Team", qCode)

      verify { mockSentryService.captureErrorMessage("404 returned when finding staff members for qcode 'Q123'") }
    }
  }
}
