package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThatCasResult

class StaffMemberServiceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val staffMemberService = StaffMemberService(mockApDeliusContextApiClient)

  @Nested
  inner class GetStaffMemberByCode {
    @Test
    fun `it returns a staff member`() {
      val staffDetail = StaffDetailFactory.staffDetail()

      every { mockApDeliusContextApiClient.getStaffDetailByStaffCode("Code123") } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = staffDetail,
      )

      val result = staffMemberService.getStaffMemberByCode("Code123")

      assertThatCasResult(result).isSuccess().with {
        assertThat(it).isEqualTo(staffDetail)
      }
    }

    @Test
    fun `it returns Unauthorised when Delius returns Unauthorised`() {
      every { mockApDeliusContextApiClient.getStaffDetailByStaffCode("Code123") } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/",
        HttpStatus.UNAUTHORIZED,
        body = null,
      )

      val result = staffMemberService.getStaffMemberByCode("Code123")

      assertThatCasResult(result).isUnauthorised()
    }

    @Test
    fun `it returns NotFound when Delius returns NotFound`() {
      every { mockApDeliusContextApiClient.getStaffDetailByStaffCode("Code123") } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      val result = staffMemberService.getStaffMemberByCode("Code123")

      assertThatCasResult(result).isNotFound("StaffMember", "Code123")
    }
  }
}
