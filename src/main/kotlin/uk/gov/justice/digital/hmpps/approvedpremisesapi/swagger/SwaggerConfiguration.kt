package uk.gov.justice.digital.hmpps.approvedpremisesapi.swagger

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem

@Configuration
class SwaggerConfiguration {

  companion object {
    init {
      io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true
    }
  }

  @Bean
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
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas1Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1Shared")
    .displayName("CAS1 & Shared")
    .pathsToExclude("/**/cas2/**", "/**/cas2v2/**", "/**/cas3/**", "/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas1DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1DomainEvents")
    .displayName("CAS1 Domain Events")
    .pathsToExclude("/**/events/cas2/**", "/**/events/cas3/**")
    .pathsToMatch("/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2Shared")
    .displayName("CAS2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2v2/**", "/**/cas3/**", "/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas2(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2")
    .displayName("CAS2")
    .pathsToMatch("/**/cas2/**")
    .pathsToExclude("/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas2DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2DomainEvents")
    .displayName("CAS2 Domain Events")
    .pathsToMatch("/**/events/cas2/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas2v2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2v2Shared")
    .displayName("CAS2v2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas3/**", "/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas3Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3Shared")
    .displayName("CAS3 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas2v2/**", "/**/events/**")
    .addOpenApiCustomizer(errorResponsesCustomizer())
    .build()

  @Bean
  fun cas3DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3DomainEvents")
    .displayName("CAS3 Domain Events")
    .pathsToMatch("/**/events/cas3/**")
    .build()

  @Bean
  fun errorResponsesCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi: OpenAPI ->
    openApi.paths.values.forEach { pathItem ->
      pathItem.readOperations().forEach { operation ->
        val responses = operation.responses

        addDefaultErrorResponse(responses, "401", "Not authenticated")
        addDefaultErrorResponse(responses, "403", "Unauthorized")
        addDefaultErrorResponse(responses, "500", "Unexpected error")
      }
    }
  }

  private fun createProblemSchema(): Schema<*> = ModelConverters.getInstance()
    .resolveAsResolvedSchema(AnnotatedType(Problem::class.java))
    .schema

  private fun addDefaultErrorResponse(
    responses: MutableMap<String, ApiResponse>,
    code: String,
    description: String,
  ) {
    if (!responses.containsKey(code)) {
      responses[code] = ApiResponse()
        .description(description)
        .content(
          Content().addMediaType(
            "application/json",
            MediaType().schema(createProblemSchema()),
          ),
        )
    }
  }
}
