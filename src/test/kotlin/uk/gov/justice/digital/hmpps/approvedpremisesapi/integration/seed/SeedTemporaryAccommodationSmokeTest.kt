package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedTemporaryAccommodationSmokeTest : SeedTestBase() {
  @Test
  fun `Temporary Accommodation premises and bedspaces can be seeded using realistic data`() {
    withCsv(
      "ta-smoketest-premises",
      "Property reference,Address Line 1,Address Line 2 (optional),City/Town,Postcode,Region,Local authority / Borough,Probation delivery unit (PDU),Floor level access?,Wheelchair accessible?,Pub nearby?,Park nearby?,School nearby?,Women only?,Men only?,Not suitable for RSO?,Not suitable for arson offenders?,Optional notes,Email Address\n" +
        "WED,1 Wednesday Street,,Westminster,SE19 4EP,London,Lewisham,Lewisham and Bromley,TRUE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,\"Property is located in a block of flats. Bedspace is accessible for wheelchair users, as has ground floor access and a lift up to the property. Cleaning turn around is 7 days.\",wed@emailaddress.com\n" +
        "CHE,1 CherryTree Lane,,Sheffield,SH7 4PB,Yorkshire and the Humber,Sheffield,Sheffield,FALSE,FALSE,FALSE,TRUE,TRUE,FALSE,TRUE,TRUE,FALSE,\"Property is located on the same road as a primary school and a park.\",che@emailaddress.com\n" +
        "SIL,12 Silverhill Lane,Broadheath,Bath,BA3 0EZ,South West,Bath and North East Somerset,Bath and North Somerset (Bath and North East Somerset and North Somerset),FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,FALSE,TRUE,\"This property has 3 bedspaces, with shared kitchen facilities.\",sil@emailaddress.com\n" +
        "STR,24 Strawberry Road,City Centre,Newport,NP1 0PA,Wales,Newport,\"Gwent (Blaenau Gwent, Caerphilly, Monmouthshire, Newport, Torfaen)\",FALSE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,\"Not suitable to those who hold addictive behaviours.\",str@emailaddress.com\n" +
        "APP,78 Applemill Court,Pudding Lane,Hereford,HR6 7ZP,West Midlands,Herefordshire,\"Herefordshire, Shropshire and Telford\",TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,app@emailaddress.com",
    )

    withCsv(
      "ta-smoketest-bedspace",
      "Property reference,Bedspace reference,Single bed?,Double bed?,Shared kitchen?,Floor level access?,Lift access?,Wheelchair accessible?,Not suitable for RSO?,Not suitable for arson offenders?,Optional notes about the bedspace,Email address\n" +
        "WED,WED-1,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,FALSE,FALSE,\"Bedspace is accessible for wheelchair users, as has ground floor access and a lift up to the property.\",wed@email.com\n" +
        "WED,WED-2,TRUE,FALSE,FALSE,TRUE,TRUE,TRUE,FALSE,FALSE,\"Bedspace is accessible for wheelchair users, as has ground floor access and a lift up to the property.\",wed@email.com\n" +
        "CHE,CHE-1,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,TRUE,FALSE,\"Property is located on the same road as a primary school and a park.\",che@emailaddress.com\n" +
        "SIL,SIL-1,TRUE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,\"Bedspace is 1 of 3 in the property, with access to a shared kitchen.\",sil@email.com\n" +
        "SIL,SIL-2,TRUE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,\"Bedspace is 2 of 3 in the property, with access to a shared kitchen.\",sil@email.com\n" +
        "SIL,SIL-3,TRUE,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,TRUE,\"Bedspace is 3 of 3 in the property, with access to a shared kitchen.\",sil@email.com\n" +
        "STR,STR-1,FALSE,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,str@email.com\n" +
        "APP,APP-1,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,app@email.com\n" +
        "APP,APP-2,TRUE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,,app@email.com",
    )

    seedService.seedData(SeedFileType.temporaryAccommodationPremises, "ta-smoketest-premises.csv")
    seedService.seedData(SeedFileType.temporaryAccommodationBedspace, "ta-smoketest-bedspace.csv")

    assertThat(logEntries).noneMatch {
      it.level == "error"
    }
  }
}
