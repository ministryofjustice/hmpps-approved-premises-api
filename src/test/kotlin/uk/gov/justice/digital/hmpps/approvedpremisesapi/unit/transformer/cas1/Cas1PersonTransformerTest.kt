package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonTransformer
import java.time.LocalDate

class Cas1PersonTransformerTest {
  private val transformer = Cas1PersonTransformer()

  @Test
  fun `transformPersonToCas1PersonDetails successfully transforms full person data`() {
    val caseSummary = CaseSummaryFactory()
      .withCrn("CRN123")
      .withName(Name("John", "Doe"))
      .withDateOfBirth(LocalDate.of(1990, 1, 1))
      .withProfile(Profile(ethnicity = "White", genderIdentity = "Male", religion = "None", nationality = "British"))
      .withNomsId("A1234BC")
      .withPnc("PNC1234")
      .produce()
    val personSummaryInfoResult = PersonSummaryInfoResult.Success.Full(
      crn = caseSummary.crn,
      summary = caseSummary,
    )

    val tier = RiskTier(level = "A1", lastUpdated = LocalDate.now())

    val result = transformer.transformPersonToCas1PersonDetails(personSummaryInfoResult, tier)

    assertEquals("John Doe", result.name)
    assertEquals(LocalDate.of(1990, 1, 1), result.dateOfBirth)
    assertEquals("British", result.nationality)
    assertEquals("A1", result.tier)
    assertEquals("A1234BC", result.nomsId)
    assertEquals("PNC1234", result.pnc)
    assertEquals("White", result.ethnicity)
    assertEquals("None", result.religion)
    assertEquals("Male", result.genderIdentity)
  }

  @Test
  fun `transformPersonToCas1PersonDetails successfully transforms restricted person data`() {
    val personSummaryInfoResult = PersonSummaryInfoResult.Success.Restricted(
      crn = "CRN123",
      nomsNumber = "A1234BC",
    )

    val tier = RiskTier(level = "A1", lastUpdated = LocalDate.now())

    val result = transformer.transformPersonToCas1PersonDetails(personSummaryInfoResult, tier)

    assertEquals("LAO Person", result.name)
    assertEquals(LocalDate.of(-999999999, 1, 1), result.dateOfBirth)
    assertEquals("LAO Person", result.nationality)
    assertEquals("A1", result.tier)
    assertEquals("A1234BC", result.nomsId)
    assertEquals("LAO Person", result.pnc)
    assertEquals("LAO Person", result.ethnicity)
    assertEquals("LAO Person", result.religion)
    assertEquals("LAO Person", result.genderIdentity)
  }

  @Test
  fun `transformPersonToCas1PersonDetails throws error for unexpected type`() {
    val personSummaryInfoResult = PersonSummaryInfoResult.NotFound(crn = "CRN1")
    val tier = RiskTier(level = "A1", lastUpdated = LocalDate.now())

    val exception = assertThrows(IllegalStateException::class.java) {
      transformer.transformPersonToCas1PersonDetails(personSummaryInfoResult, tier)
    }

    assertEquals("Unexpected PersonSummaryInfoResult type: NotFound", exception.message)
  }
}
