package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.postgresql.util.PGobject
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SarRepository(
  val jdbcTemplate: NamedParameterJdbcTemplate,
) {

  fun getApplicationsJson(crn: String): String {
    val result = jdbcTemplate.queryForMap(
      """
        select 
        	json_agg(app) as json
        from 
        (
        	select 
            a.id,
            apa.name, 
            a.data from 
          applications a 
          inner join approved_premises_applications apa on a.id = apa.id
          where a.crn = :crn
        ) app;
      """.trimIndent(),
      mapOf<String, Any>("crn" to crn),
    )

    return toJsonString(result)
  }

  fun toJsonString(result: Map<String, Any>) = (result["json"] as PGobject?)?.value ?: "[]"
}
