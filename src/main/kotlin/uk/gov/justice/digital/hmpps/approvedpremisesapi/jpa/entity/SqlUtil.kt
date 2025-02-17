package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.sql.ResultSet
import java.util.UUID

object SqlUtil {
  fun ResultSet.getUUID(columnLabel: String): UUID = UUID.fromString(this.getString(columnLabel))

  fun toStringList(array: java.sql.Array?): List<String> {
    if (array == null) {
      return emptyList()
    }

    val result = (array.array as Array<String>).toList()

    return if (result.size == 1 && result[0] == null) {
      emptyList()
    } else {
      result
    }
  }
}
