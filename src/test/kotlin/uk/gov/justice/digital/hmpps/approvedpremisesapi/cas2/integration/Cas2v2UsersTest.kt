package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2DeliusUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2v2NomisUser

class Cas2v2UsersTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class GetUserDetailsTest {

    @Test
    fun `Getting a user without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/users/JIMIDELIUS")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting a user returns region information for the logged in Delius user`() {
      val staffDetail = StaffDetailFactory.staffDetail()

      givenACas2v2DeliusUser(
        staffDetail = staffDetail,
      ) { userEntity, jwt ->
        val response = webTestClient.get()
          .uri("/cas2/users/DOESNTMATTER")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody<Cas2UserDto>()
          .returnResult()
          .responseBody

        assertThat(response).isNotNull
        assertThat(response?.username).isEqualTo(userEntity.username)
        assertThat(response?.type).isEqualTo(Cas2UserTypeDto.DELIUS)
        assertThat(response?.deliusUserInfo!!.probationArea.code).isEqualTo(staffDetail.probationArea.code)
        assertThat(response.deliusUserInfo.probationArea.description).isEqualTo(staffDetail.probationArea.description)
      }
    }
  }

  @Test
  fun `Getting a user returns region information for the logged in Nomis user does not include region information`() {
    givenACas2v2NomisUser { userEntity, jwt ->
      val response = webTestClient.get()
        .uri("/cas2/users/DOESNTMATTER")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Cas2UserDto>()
        .returnResult()
        .responseBody

      assertThat(response).isNotNull
      assertThat(response?.username).isEqualTo(userEntity.username)
      assertThat(response?.type).isEqualTo(Cas2UserTypeDto.NOMIS)
      assertThat(response?.deliusUserInfo).isNull()
    }
  }

  @Test
  fun `Does not fail if there are multiple users with the same username`() {
    val nomisUser = givenACas2v2NomisUser()

    val staffDetail = StaffDetailFactory.staffDetail(
      deliusUsername = nomisUser.username,
    )

    givenACas2v2DeliusUser(
      staffDetail = staffDetail,
    ) { userEntity, jwt ->
      val response = webTestClient.get()
        .uri("/cas2/users/DOESNTMATTER")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Cas2UserDto>()
        .returnResult()
        .responseBody

      assertThat(response).isNotNull
      assertThat(response?.username).isEqualTo(userEntity.username)
      assertThat(response?.type).isEqualTo(Cas2UserTypeDto.DELIUS)
      assertThat(response?.deliusUserInfo!!.probationArea.code).isEqualTo(staffDetail.probationArea.code)
      assertThat(response.deliusUserInfo.probationArea.description).isEqualTo(staffDetail.probationArea.description)
    }
  }
}
