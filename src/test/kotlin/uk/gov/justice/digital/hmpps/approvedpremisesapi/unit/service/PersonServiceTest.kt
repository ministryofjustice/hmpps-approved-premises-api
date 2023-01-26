package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PersonService

class PersonServiceTest {
  private val mockOffenderService = mockk<OffenderService>()

  private val personService = PersonService(
    mockOffenderService
  )

  @Nested
  inner class GetPersonByCRN {
    private val crn = "a-crn"
    private val userDistinguisedName = "distinguished.name"
    private val nomsNumber = "noms-number"

    private val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    private val inmateDetail = InmateDetail(
      offenderNo = nomsNumber,
      inOutStatus = InOutStatus.IN,
      assignedLivingUnit = AssignedLivingUnit(
        agencyId = "AGY",
        locationId = 89,
        description = "AGENCY DESCRIPTION",
        agencyName = "AGENCY NAME"
      )
    )

    @Test
    fun `getPersonByCRN returns NotFound result when the offenderDetails cannot be found`() {
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.NotFound()

      val result = personService.getPersonByCrn(
        crn,
        userDistinguisedName
      )

      Assertions.assertThat(
        result is AuthorisableActionResult.NotFound && result.id == crn && result.entityType == "Person"
      ).isTrue
    }

    @Test
    fun `getPersonByCRN returns Unauthorised result when the user is not authorised to access the offenderDetails`() {
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.Unauthorised()

      val result = personService.getPersonByCrn(
        crn,
        userDistinguisedName
      )

      Assertions.assertThat(
        result is AuthorisableActionResult.Unauthorised
      ).isTrue
    }

    @Test
    fun `getPersonByCRN returns NotFound result when the inmateDetails cannot be found`() {
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockOffenderService.getInmateDetailByNomsNumber(nomsNumber) } returns AuthorisableActionResult.NotFound()

      val result = personService.getPersonByCrn(
        crn,
        userDistinguisedName
      )

      Assertions.assertThat(
        result is AuthorisableActionResult.NotFound && result.id == nomsNumber && result.entityType == "Inmate"
      ).isTrue
    }

    @Test
    fun `getPersonByCRN returns NotFound result when the user is not authorised to access the inmateDetails`() {
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockOffenderService.getInmateDetailByNomsNumber(nomsNumber) } returns AuthorisableActionResult.Unauthorised()

      val result = personService.getPersonByCrn(
        crn,
        userDistinguisedName
      )

      Assertions.assertThat(
        result is AuthorisableActionResult.Unauthorised
      ).isTrue
    }

    @Test
    fun `getPersonByCRN throws an error when the offender does not have a NOMS number`() {
      val offenderWithoutNomsNumber = OffenderDetailsSummaryFactory()
        .withoutNomsNumber()
        .produce()
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.Success(offenderWithoutNomsNumber)

      val exception = assertThrows<RuntimeException> { personService.getPersonByCrn(crn, userDistinguisedName) }
      Assertions.assertThat(exception.message).isEqualTo("No nomsNumber present for CRN: `$crn`")
    }

    @Test
    fun `getPersonByCRN returns a pair of OffenderDetails and InmateDetails`() {
      every { mockOffenderService.getOffenderByCrn(crn, userDistinguisedName) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockOffenderService.getInmateDetailByNomsNumber(nomsNumber) } returns AuthorisableActionResult.Success(inmateDetail)

      val result = personService.getPersonByCrn(
        crn,
        userDistinguisedName
      )

      Assertions.assertThat(
        result is AuthorisableActionResult.Success && result.entity == Pair(offenderDetails, inmateDetail)
      ).isTrue
    }
  }
}
