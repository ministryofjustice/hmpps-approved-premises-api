package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedTemporaryAccommodationSmokeTest : SeedTestBase() {
  @Test
  fun `Temporary Accommodation premises and bedspaces can be seeded using realistic data`() {
    seed(
      SeedFileType.temporaryAccommodationPremises,
      """
        Property reference,Address Line 1,Address Line 2 (optional),City/Town,Postcode,Region,Local authority / Borough,Probation delivery unit (PDU),Floor level access?,Wheelchair accessible?,Pub nearby?,Park nearby?,School nearby?,Women only?,Men only?,Not suitable for RSO?,Not suitable for arson offenders?,Optional notes,Email Address,Start Date,End Date
        WED,1 Wednesday Street,,Westminster,SE19 4EP,London,Lewisham,Lewisham and Bromley,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,"Property is located in a block of flats. Bedspace is accessible for wheelchair users, as has ground floor access and a lift up to the property. Cleaning turn around is 7 days.",wed@emailaddress.com,2025-01-01,2025-01-02
        CHE,1 CherryTree Lane,,Sheffield,SH7 4PB,Yorkshire and the Humber,Sheffield,Sheffield,FALSE,FALSE,FALSE,TRUE,TRUE,FALSE,TRUE,TRUE,FALSE,"Property is located on the same road as a primary school and a park.",che@emailaddress.com,2025-01-01,
        SIL,12 Silverhill Lane,Broadheath,Bath,BA3 0EZ,South West,Bath and North East Somerset,Bath and North Somerset (Bath and North East Somerset and North Somerset),FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,FALSE,TRUE,"This property has 3 bedspaces, with shared kitchen facilities.",sil@emailaddress.com,2025-01-01,
        STR,24 Strawberry Road,City Centre,Newport,NP1 0PA,Wales,Newport,"Gwent (Blaenau Gwent, Caerphilly, Monmouthshire, Newport, Torfaen)",FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,"Not suitable to those who hold addictive behaviours.",str@emailaddress.com,2025-01-01,
        APP,78 Applemill Court,Pudding Lane,Hereford,HR6 7ZP,West Midlands,Herefordshire,"Herefordshire, Shropshire and Telford",TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,app@emailaddress.com,2025-01-01,
      """.trimIndent(),
    )

    assertThat(logEntries).noneMatch {
      it.level == "error"
    }
  }
}
