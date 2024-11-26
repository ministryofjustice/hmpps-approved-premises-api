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
  fun allCas(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("allCas")
      .displayName("All CAS")
      .pathsToMatch("/**")
      .build()
  }

  @Bean
  fun shared(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("shared")
      .displayName("Shared")
      .pathsToMatch("/**")
      .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas3/**")
      .build()
  }

  @Bean
  fun events(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("domainEvents")
      .displayName("Domain Events")
      .pathsToMatch("/events/**")
      .build()
  }

  @Bean
  fun cas2Events(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2DomainEvents")
      .displayName("CAS2 Domain Events")
      .pathsToMatch("/events/cas2/**")
      .build()
  }

  @Bean
  fun cas1Shared(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS1Shared")
      .displayName("CAS1 & Shared")
      .pathsToMatch("/**", "/**/cas1/**")
      .pathsToExclude("/**/cas2/**", "/**/cas3/**")
      .build()
  }

  @Bean
  fun cas2Shared(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2Shared")
      .displayName("CAS2 & Shared")
      .pathsToMatch("/**", "/**/cas2/**")
      .pathsToExclude("/**/cas1/**", "/**/cas3/**")
      .build()
  }

  @Bean
  fun cas3Shared(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS3Shared")
      .displayName("CAS3 & Shared")
      .pathsToMatch("/**", "/**/cas3/**")
      .pathsToExclude("/**/cas1/**", "/**/cas2/**")
      .build()
  }

  @Bean
  fun cas1Only(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS1")
      .displayName("CAS1")
      .pathsToMatch("/**/cas1/**")
      .build()
  }

  @Bean
  fun cas2Only(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2")
      .displayName("CAS2")
      .pathsToMatch("/**/cas2/**")
      .build()
  }

  @Bean
  fun cas3(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS3")
      .displayName("CAS3")
      .pathsToMatch("/**/cas3/**")
      .build()
  }
}
