package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

import io.gatling.javaapi.core.Session
import java.util.UUID

fun Session.getUUID(key: String) = UUID.fromString(this.getString(key))
