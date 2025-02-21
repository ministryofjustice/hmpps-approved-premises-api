package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SwaggerConfiguration {

  companion object {
    init {
      io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true
    }
  }

  @Bean
  @Primary
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .info(
      Info()
        .title("Approved Premises")
        .version("1.0.0")
        .description("Swagger Documentation for Approved Premises API"),
    )

  @Bean
  fun allCas(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("allCas")
    .displayName("All CAS")
    .pathsToMatch("/**")
    .build()

  @Bean
  fun cas1Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1Shared")
    .displayName("CAS1 & Shared")
    .pathsToExclude("/**/cas2/**", "/**/cas3/**", "/**/events/**")
    .build()

  @Bean
  fun cas1DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1DomainEvents")
    .displayName("CAS1 Domain Events")
    .pathsToExclude("/**/events/cas2/**", "/**/events/cas3/**")
    .pathsToMatch("/**/events/**")
    .build()

  @Bean
  fun cas2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2Shared")
    .displayName("CAS2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas3/**", "/**/events/**")
    .build()

  @Bean
  fun cas2DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2DomainEvents")
    .displayName("CAS2 Domain Events")
    .pathsToMatch("/**/events/cas2/**")
    .build()

  @Bean
  fun cas2v2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2v2Shared")
    .displayName("CAS2v2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas3/**", "/**/events/**")
    .build()

  @Bean
  fun cas3Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3Shared")
    .displayName("CAS3 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/events/**")
    .build()

  @Bean
  fun cas3DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3DomainEvents")
    .displayName("CAS3 Domain Events")
    .pathsToMatch("/**/events/cas3/**")
    .build()
}
