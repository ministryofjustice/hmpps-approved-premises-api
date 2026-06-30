package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManageUsersApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.NomisUserRolesForRequesterApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

class Cas2UserServiceTest {
  private val mockHttpAuthService = mockk<HttpAuthService>()
  private val mockCas2UserRepository = mockk<Cas2UserRepository>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockNomisUserRolesForRequesterApiClient = mockk<NomisUserRolesForRequesterApiClient>()
  private val mockManageUsersApiClient = mockk<ManageUsersApiClient>()
  private val cas2UserService = Cas2UserService(
    mockHttpAuthService,
    mockCas2UserRepository,
    mockApDeliusContextApiClient,
    mockNomisUserRolesForRequesterApiClient,
    mockManageUsersApiClient,
  )

  @Nested
  inner class GetCurrentUserTest {
    @Test
    fun `returns error when Delius user look up fails`() {
      val username = "DELIUSUSER"
      val deliusUser = Cas2UserEntityFactory()
        .withUsername(username)
        .withUserType(Cas2UserType.DELIUS)
        .produce()

      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      every { mockHttpAuthService.getCas2v2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.token.tokenValue } returns "abc123"
      every { mockPrincipal.authenticationSource() } returns "delius"
      every { mockPrincipal.name } returns username
      every {
        mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.DELIUS, deliusUser.serviceOrigin)
      } returns deliusUser

      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/staff/username/$username",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      assertThrows<RuntimeException> { cas2UserService.getUserDtoForRequest() }
    }

    @Test
    fun `returns success when user is Delius user`() {
      val username = "DELIUSUSER"
      val deliusUser = Cas2UserEntityFactory()
        .withUsername(username)
        .withUserType(Cas2UserType.DELIUS)
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()
      val staffDetail = StaffDetailFactory.staffDetail(
        probationArea = ProbationArea(
          code = "PA01",
          description = "probation area description",
        ),
      )

      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      every { mockHttpAuthService.getCas2v2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.token.tokenValue } returns "abc123"
      every { mockPrincipal.authenticationSource() } returns "delius"
      every { mockPrincipal.name } returns username
      every { mockApDeliusContextApiClient.getStaffDetail(username) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = staffDetail,
      )
      every {
        mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.DELIUS, deliusUser.serviceOrigin)
      } returns deliusUser
      every { mockCas2UserRepository.save(any()) } returns deliusUser

      val userDto = extractEntityFromCasResult(cas2UserService.getUserDtoForRequest())
      assertThat(userDto.username).isEqualTo("DELIUSUSER")
      assertThat(userDto.type).isEqualTo(Cas2UserTypeDto.DELIUS)
      assertThat(userDto.deliusUserInfo!!.probationArea.code).isEqualTo("PA01")
      assertThat(userDto.deliusUserInfo.probationArea.description).isEqualTo("probation area description")
    }

    @Test
    fun `returns error when Nomis user look up fails`() {
      val username = "NOMISUSER"
      Cas2UserEntityFactory()
        .withUsername(username)
        .withUserType(Cas2UserType.NOMIS)
        .produce()

      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      every { mockHttpAuthService.getCas2v2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.token.tokenValue } returns "abc123"
      every { mockPrincipal.authenticationSource() } returns "nomis"
      every { mockPrincipal.name } returns username
      every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/me",
        HttpStatus.NOT_FOUND,
        body = null,
      )

      assertThrows<RuntimeException> { cas2UserService.getUserDtoForRequest() }
    }

    @Test
    fun `returns success when user is Nomis user`() {
      val username = "NOMISUSER"
      val nomisUser = Cas2UserEntityFactory()
        .withUsername(username)
        .withUserType(Cas2UserType.NOMIS)
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()
      val nomisUserDetail = NomisUserDetailFactory().produce()

      val mockPrincipal = mockk<AuthAwareAuthenticationToken>()
      every { mockHttpAuthService.getCas2v2AuthenticatedPrincipalOrThrow() } returns mockPrincipal
      every { mockPrincipal.token.tokenValue } returns "abc123"
      every { mockPrincipal.authenticationSource() } returns "nomis"
      every { mockPrincipal.name } returns username
      every { mockNomisUserRolesForRequesterApiClient.getUserDetailsForMe("abc123") } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = nomisUserDetail,
      )
      every {
        mockCas2UserRepository.findByUsernameAndUserTypeAndServiceOrigin(username, Cas2UserType.NOMIS, nomisUser.serviceOrigin)
      } returns nomisUser
      every { mockCas2UserRepository.save(nomisUser) } returns nomisUser

      val userDto = extractEntityFromCasResult(cas2UserService.getUserDtoForRequest())
      assertThat(userDto.username).isEqualTo("NOMISUSER")
      assertThat(userDto.type).isEqualTo(Cas2UserTypeDto.NOMIS)
      assertThat(userDto.deliusUserInfo).isNull()
    }
  }
}
