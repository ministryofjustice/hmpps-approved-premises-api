package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer

class DomainEventTransformerTest {

  private val communityApiClient = mockk<CommunityApiClient>()

  val domainEventTransformerService = DomainEventTransformer(communityApiClient)

  @Test
  fun `toProbationArea success`() {
    val staffDetails = StaffUserDetailsFactory()
      .withProbationAreaCode("theProbationCode")
      .withProbationAreaDescription("theProbationDescription")
      .produce()

    val result = domainEventTransformerService.toProbationArea(staffDetails)

    assertThat(result.code).isEqualTo("theProbationCode")
    assertThat(result.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toStaffMember success`() {
    val staffDetails = StaffUserDetailsFactory()
      .withStaffCode("theStaffCode")
      .withStaffIdentifier(22L)
      .withForenames("theForenames")
      .withSurname("theSurname")
      .withUsername("theUsername")
      .produce()

    val result = domainEventTransformerService.toStaffMember(staffDetails)

    assertThat(result.staffCode).isEqualTo("theStaffCode")
    assertThat(result.staffIdentifier).isEqualTo(22L)
    assertThat(result.forenames).isEqualTo("theForenames")
    assertThat(result.surname).isEqualTo("theSurname")
    assertThat(result.username).isEqualTo("theUsername")
  }

  @Test
  fun `toStaffMember(UserEntity) returns successfully`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withDeliusStaffCode("theStaffCode")
      .withDeliusStaffIdentifier(22L)
      .withName("theForenames theSurname")
      .withDeliusUsername("theUsername")
      .produce()

    val staffDetails = StaffUserDetailsFactory()
      .withStaffCode("theStaffCode")
      .withStaffIdentifier(22L)
      .withForenames("theForenames")
      .withSurname("theSurname")
      .withUsername("theUsername")
      .produce()

    every { communityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(HttpStatus.OK, staffDetails)

    val result = domainEventTransformerService.toStaffMember(user)

    assertThat(result.staffCode).isEqualTo("theStaffCode")
    assertThat(result.staffIdentifier).isEqualTo(22L)
    assertThat(result.forenames).isEqualTo("theForenames")
    assertThat(result.surname).isEqualTo("theSurname")
    assertThat(result.username).isEqualTo("theUsername")
  }

  @Test
  fun `toStaffMember(UserEntity) fails`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .withDeliusStaffCode("theStaffCode")
      .withDeliusStaffIdentifier(22L)
      .withName("theForenames theSurname")
      .withDeliusUsername("theUsername")
      .produce()

    val response = ClientResult.Failure.StatusCode<StaffUserDetails>(
      HttpMethod.GET,
      "/",
      HttpStatus.BAD_REQUEST,
      "",
    )

    val expectedException = response.toException()

    every { communityApiClient.getStaffUserDetails(user.deliusUsername) } returns response

    val exception = assertThrows<RuntimeException> { domainEventTransformerService.toStaffMember(user) }

    assertThat(exception.message).isEqualTo(expectedException.message)
  }

  @Test
  fun `toWithdrawnBy from staff details success`() {
    val staffDetails = StaffUserDetailsFactory()
      .withStaffCode("theStaffCode")
      .withStaffIdentifier(22L)
      .withForenames("theForenames")
      .withSurname("theSurname")
      .withUsername("theUsername")
      .withProbationAreaCode("theProbationCode")
      .withProbationAreaDescription("theProbationDescription")
      .produce()

    val result = domainEventTransformerService.toWithdrawnBy(staffDetails)

    val staffMember = result.staffMember
    assertThat(staffMember.staffCode).isEqualTo("theStaffCode")
    assertThat(staffMember.staffIdentifier).isEqualTo(22L)
    assertThat(staffMember.forenames).isEqualTo("theForenames")
    assertThat(staffMember.surname).isEqualTo("theSurname")
    assertThat(staffMember.username).isEqualTo("theUsername")

    val probationArea = result.probationArea
    assertThat(probationArea.code).isEqualTo("theProbationCode")
    assertThat(probationArea.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toWithdrawnBy from user success`() {
    val staffDetails = StaffUserDetailsFactory()
      .withStaffCode("theStaffCode")
      .withStaffIdentifier(22L)
      .withForenames("theForenames")
      .withSurname("theSurname")
      .withUsername("theUsername")
      .withProbationAreaCode("theProbationCode")
      .withProbationAreaDescription("theProbationDescription")
      .produce()

    val user = UserEntityFactory().withDefaultProbationRegion().produce()

    every {
      communityApiClient.getStaffUserDetails(user.deliusUsername)
    } returns ClientResult.Success(HttpStatus.OK, staffDetails)

    val result = domainEventTransformerService.toWithdrawnBy(user)

    val staffMember = result.staffMember
    assertThat(staffMember.staffCode).isEqualTo("theStaffCode")
    assertThat(staffMember.staffIdentifier).isEqualTo(22L)
    assertThat(staffMember.forenames).isEqualTo("theForenames")
    assertThat(staffMember.surname).isEqualTo("theSurname")
    assertThat(staffMember.username).isEqualTo("theUsername")

    val probationArea = result.probationArea
    assertThat(probationArea.code).isEqualTo("theProbationCode")
    assertThat(probationArea.name).isEqualTo("theProbationDescription")
  }

  @Test
  fun `toWithdrawnBy from user failure`() {
    val user = UserEntityFactory().withDefaultProbationRegion().produce()

    every {
      communityApiClient.getStaffUserDetails(user.deliusUsername)
    } returns Failure.CachedValueUnavailable(user.deliusUsername)

    assertThatThrownBy { domainEventTransformerService.toWithdrawnBy(user) }
  }
}
