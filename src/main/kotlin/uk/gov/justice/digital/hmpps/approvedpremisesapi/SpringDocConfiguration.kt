package uk.gov.justice.digital.hmpps.approvedpremisesapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SpringDocConfiguration {

  @Bean
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
  fun public(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("public")
      .pathsToMatch("/**")
      .build()
  }

  @Bean
  fun cas1(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS1")
      .pathsToMatch("**/cas1/**", "/**")
      .pathsToExclude("**/cas2/**", "**/cas3/**")
      .build()
  }

  @Bean
  fun cas2(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS2")
      .pathsToMatch("**/cas2/**", "/**")
      .pathsToExclude("**/cas1/**", "**/cas3/**")
      .build()
  }

  @Bean
  fun cas3(): GroupedOpenApi {
    return GroupedOpenApi.builder()
      .group("CAS3")
      .pathsToMatch("**/cas3/**", "/**")
      .pathsToExclude("**/cas1/**", "**/cas2/**")
      .build()
  }
}
