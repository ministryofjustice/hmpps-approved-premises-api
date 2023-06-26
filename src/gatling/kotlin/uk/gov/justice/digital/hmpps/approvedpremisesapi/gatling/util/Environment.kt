package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

val BASE_URL = System.getenv("GATLING_BASE_URL") ?: "http://localhost:8080"
val USERNAME = System.getenv("GATLING_USERNAME") ?: "JimSnowLdap"
val PASSWORD = System.getenv("GATLING_PASSWORD") ?: "secret"
val HMPPS_AUTH_BASE_URL = System.getenv("GATLING_HMPPS_AUTH_BASE_URL") ?: "http://localhost:9091"
