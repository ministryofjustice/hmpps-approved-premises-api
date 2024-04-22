package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

val BASE_URL = System.getenv("GATLING_BASE_URL") ?: "http://localhost:8080"
val HMPPS_AUTH_BASE_URL = System.getenv("GATLING_HMPPS_AUTH_BASE_URL") ?: "http://localhost:9091"
val CRN = System.getenv("GATLING_CRN") ?: "X320741"

val CAS1_USERNAME = System.getenv("GATLING_CAS1_USERNAME") ?: "JimSnowLdap"
val CAS1_PASSWORD = System.getenv("GATLING_CAS1_PASSWORD") ?: "secret"

val CAS2_USERNAME = System.getenv("GATLING_CAS2_USERNAME") ?: ""
val CAS2_PASSWORD = System.getenv("GATLING_CAS2_PASSWORD") ?: ""

val CAS3_USERNAME = System.getenv("GATLING_CAS3_USERNAME") ?: "JimSnowLdap"
val CAS3_PASSWORD = System.getenv("GATLING_CAS3_PASSWORD") ?: "secret"
