package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SarService

class SarServiceTest : IntegrationTestBase() {

  companion object {
    const val CRN = "CRN123"
    const val NAME = "Jeffity Jeff"
  }

  @Autowired
  lateinit var sarService: SarService

  @Test
  fun `Get CAS1 Information - No Results`() {
    val result = sarService.getSarResult(CRN)

    assertJsonEquals(
      """ {
          "applications": [ ]
        }
      """,
      result,
    )
  }

  @Test
  fun `Get CAS1 Information - Have Application`() {
    val (user, _) = `Given a User`()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(CRN)
      withName(NAME)
      withCreatedByUser(user)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withData("""{ "key": "value" }""")
    }

    val result = sarService.getSarResult(CRN)

    val expectedJson = """
      {
        "applications": 
        [
          {
            "id": "${application.id}",
            "name": "$NAME",
            "data": { 
              "key": "value"
            }
          }
        ]
      }
    """.trimIndent()

    assertJsonEquals(
      expectedJson,
      result,
    )
  }

  private fun assertJsonEquals(
    expected: String,
    actual: String,
  ) {
    JSONAssert.assertEquals(
      expected.trimMargin(),
      actual,
      JSONCompareMode.NON_EXTENSIBLE,
    )
  }
}
