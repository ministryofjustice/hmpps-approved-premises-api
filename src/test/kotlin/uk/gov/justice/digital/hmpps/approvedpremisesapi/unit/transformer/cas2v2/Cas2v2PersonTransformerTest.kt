package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2PersonTransformer

class Cas2v2PersonTransformerTest {

  private val cas2v2PersonTransformer = Cas2v2PersonTransformer()

  private val crn = "crn123"
  private val nomsNumber = "noms123"

  @Test
  fun `transformProbationOffenderDetailAndInmateDetailToFullPerson transforms correctly for a full person info`() {
    val probationOffenderDetails = ProbationOffenderDetailFactory()
      .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
      .withFirstName("John")
      .withSurname("Smith")
      .produce()

    val fullPerson: FullPerson = cas2v2PersonTransformer.transformProbationOffenderDetailAndInmateDetailToFullPerson(probationOffenderDetails)
    assertThat(fullPerson.crn).isEqualTo(probationOffenderDetails.otherIds.crn)
    assertThat(fullPerson.nomsNumber).isEqualTo(probationOffenderDetails.otherIds.nomsNumber)
    assertThat(fullPerson.status).isEqualTo(PersonStatus.unknown)
    assertThat(fullPerson.name).isEqualTo("John Smith")
    assertThat(fullPerson.type).isEqualTo(PersonType.fullPerson)
  }

  @Test
  fun `transformCaseSummaryToFullPerson transforms correctly for a full person info`() {
    val caseSummary = CaseSummaryFactory()
      .withName(Name(forename = "John", surname = "Smith", middleNames = emptyList()))
      .withCrn(crn)
      .withNomsId(nomsNumber)
      .produce()

    val fullPerson: FullPerson = cas2v2PersonTransformer.transformCaseSummaryToFullPerson(caseSummary)
    assertThat(fullPerson.crn).isEqualTo(caseSummary.crn)
    assertThat(fullPerson.nomsNumber).isEqualTo(caseSummary.nomsId)
    assertThat(fullPerson.status).isEqualTo(PersonStatus.unknown)
    assertThat(fullPerson.name).isEqualTo("John Smith")
    assertThat(fullPerson.type).isEqualTo(PersonType.fullPerson)
  }
}
