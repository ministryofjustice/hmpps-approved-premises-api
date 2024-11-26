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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService

class StaffMemberServiceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val staffMemberService = StaffMemberService(mockApDeliusContextApiClient)

  private val qCode = "Qcode"

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

      val result = staffMemberService.getStaffMemberByCode(staffMembers[2].code, qCode)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      assertThat(result.value).isEqualTo(staffMembers[2])
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

    val result = staffMemberService.getStaffMemberByCode("code", qCode)

    assertThat(result is CasResult.Unauthorised).isTrue
  }

  @Test
  fun `it returns NotFound when Delius returns NotFound`() {
    every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Failure.StatusCode(
      HttpMethod.GET,
      "/staff-members/code",
      HttpStatus.NOT_FOUND,
      body = null,
    )

    val result = staffMemberService.getStaffMemberByCode("code", qCode)

    assertThat(result is CasResult.NotFound).isTrue
    result as CasResult.NotFound

    assertThat(result.id).isEqualTo(qCode)
    assertThat(result.entityType).isEqualTo("Team")
  }

  @Test
  fun `it returns a NotFound when a staff member for the QCode cannot me found`() {
    val staffMembers = ContextStaffMemberFactory().produceMany().take(5).toList()

    every { mockApDeliusContextApiClient.getStaffMembers(qCode) } returns ClientResult.Success(
      status = HttpStatus.OK,
      body = StaffMembersPage(
        content = staffMembers,
      ),
    )

    val result = staffMemberService.getStaffMemberByCode("code", qCode)

    assertThat(result is CasResult.NotFound).isTrue
    result as CasResult.NotFound

    assertThat(result.id).isEqualTo("code")
    assertThat(result.entityType).isEqualTo("Staff Code")
  }
}
