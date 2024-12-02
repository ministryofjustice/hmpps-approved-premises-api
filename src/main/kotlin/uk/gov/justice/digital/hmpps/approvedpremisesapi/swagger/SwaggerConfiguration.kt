package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class SwaggerConfiguration {

  @Bean
  @Primary
  fun customOpenAPI(): OpenAPI {
    return OpenAPI()
      .info(
        Info()
          .title("Approved Premises")
          .version("1.0.0")
          .description("Swagger Documentation for Approved Premises API"),
      )
  }

  @Bean
  fun shared(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("shared")
      .displayName("Shared")
      .pathsToMatch("/**")
      .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas3/**", "/**/events/**")
      .packagesToExclude(
        "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events",
      )
      .build()
  }

  @Bean
  fun events(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("domainEvents")
      .displayName("Domain Events")
      .pathsToMatch("/**/events/**")
      .build()
  }

  @Bean
  fun cas2Events(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2DomainEvents")
      .displayName("CAS2 Domain Events")
      .pathsToMatch("/**/events/cas2/**")
      .build()
  }

  @Bean
  fun cas1Only(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS1")
      .displayName("CAS1")
      .pathsToMatch("/**/cas1/**")
      .pathsToExclude("/events/**", "**/cas2/**", "**/cas3/**")
      .packagesToExclude(
        "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.**",
        "**.cas2.**",
        "**.cas3.**",
      )
      .build()
  }

  @Bean
  fun cas2Only(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2")
      .displayName("CAS2")
      .pathsToMatch("/**/cas2/**")
      .pathsToExclude("/events/**", "**/cas1/**", "**/cas3/**")
      .packagesToExclude(
        "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.**",
        "**.cas1.**",
        "**.cas3.**",
      ).build()
  }

  @Bean
  fun cas3Only(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS3")
      .displayName("CAS3")
      .pathsToMatch("/**/cas3/**")
      .pathsToExclude("/events/**", "/**/cas1/**", "/**/cas2/**")
      .packagesToExclude(
        "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.**",
        "**.cas1.**",
        "**.cas2.**",
      )
      .build()
  }
}
