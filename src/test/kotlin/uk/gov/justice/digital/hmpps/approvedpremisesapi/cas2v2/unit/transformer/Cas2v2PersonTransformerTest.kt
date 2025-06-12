package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProfileFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name

class Cas2v2PersonTransformerTest {

  private val cas2v2PersonTransformer = Cas2v2PersonTransformer()

  private val crn = "crn123"
  private val nomsNumber = "noms123"
  private val nationality = "Martian"

  @Test
  fun `transformCaseSummaryToFullPerson transforms correctly for a full person info`() {
    val caseSummary = CaseSummaryFactory()
      .withName(Name(forename = "John", surname = "Smith", middleNames = emptyList()))
      .withCrn(crn)
      .withNomsId(nomsNumber)
      .withProfile(
        ProfileFactory().withNationality(nationality).produce(),
      )
      .produce()

    val fullPerson: FullPerson = cas2v2PersonTransformer.transformCaseSummaryToFullPerson(caseSummary)
    assertThat(fullPerson.crn).isEqualTo(caseSummary.crn)
    assertThat(fullPerson.nomsNumber).isEqualTo(caseSummary.nomsId)
    assertThat(fullPerson.status).isEqualTo(PersonStatus.unknown)
    assertThat(fullPerson.name).isEqualTo("John Smith")
    assertThat(fullPerson.type).isEqualTo(PersonType.fullPerson)
    assertThat(fullPerson.nationality).isEqualTo(nationality)
  }
}
