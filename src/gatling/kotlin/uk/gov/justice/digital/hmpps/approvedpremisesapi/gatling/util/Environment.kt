package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util

val BASE_URL = System.getenv("GATLING_BASE_URL") ?: "http://localhost:8080"
val USERNAME = System.getenv("GATLING_USERNAME") ?: "JimSnowLdap"
val PASSWORD = System.getenv("GATLING_PASSWORD") ?: "secret"
val HMPPS_AUTH_BASE_URL = System.getenv("GATLING_HMPPS_AUTH_BASE_URL") ?: "http://localhost:9091"
val HMPPS_AUTH_CLIENT_ID = System.getenv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_AP-OASYS-CONTEXT_CLIENT-ID") ?: "gatling_client_id"
val HMPPS_AUTH_CLIENT_SECRET = System.getenv("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_AP-OASYS-CONTEXT_CLIENT-SECRET") ?: "gatling_client_secret"
val CRN = System.getenv("GATLING_CRN") ?: "X320741"
