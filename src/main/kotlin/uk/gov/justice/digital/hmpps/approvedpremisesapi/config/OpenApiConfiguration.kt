package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem

@SecurityScheme(
  name = "bearer-jwt",
  type = SecuritySchemeType.HTTP,
  scheme = "bearer",
  bearerFormat = "JWT",
)
@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version!!

  companion object {
    init {
      ModelResolver.enumsAsRef = true
    }
  }

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .info(
      Info()
        .title("Approved Premises")
        .version(version)
        .description("Swagger Documentation for Approved Premises API"),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt"))

  @Bean
  fun allCas(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("allCas")
    .displayName("All CAS")
    .pathsToMatch("/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas1Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1Shared")
    .displayName("CAS1 & Shared")
    .pathsToExclude("/**/cas2/**", "/**/cas2v2/**", "/**/cas3/**", "/**/events/**", "/queue-admin/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas1DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS1DomainEvents")
    .displayName("CAS1 Domain Events")
    .pathsToExclude("/**/events/cas2/**", "/**/events/cas3/**", "/queue-admin/**")
    .pathsToMatch("/**/events/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2Shared")
    .displayName("CAS2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2v2/**", "/**/cas3/**", "/**/events/**", "/queue-admin/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas2(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2")
    .displayName("CAS2")
    .pathsToMatch("/**/cas2/**", "/**/cas2-hdc/**")
    .pathsToExclude("/**/events/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas2DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2DomainEvents")
    .displayName("CAS2 Domain Events")
    .pathsToMatch("/**/events/cas2/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas2v2Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS2v2Shared")
    .displayName("CAS2v2 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas3/**", "/**/events/**", "/queue-admin/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas3Shared(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3Shared")
    .displayName("CAS3 & Shared")
    .pathsToExclude("/**/cas1/**", "/**/cas2/**", "/**/cas2v2/**", "/**/events/**", "/queue-admin/**")
    .addOpenApiCustomizer(openApiCustomizer())
    .build()

  @Bean
  fun cas3DomainEvents(): GroupedOpenApi = GroupedOpenApi.builder()
    .group("CAS3DomainEvents")
    .displayName("CAS3 Domain Events")
    .pathsToMatch("/**/events/cas3/**")
    .build()

  @SuppressWarnings("CyclomaticComplexMethod")
  @Bean
  fun openApiCustomizer() = OpenApiCustomizer { openApi: OpenAPI ->
    openApi.paths.values.forEach { pathItem ->
      pathItem.readOperations().forEach { operation ->
        val responses = operation.responses

        addDefaultErrorResponse(responses, "401", "Not authenticated")
        addDefaultErrorResponse(responses, "403", "Unauthorized")
        addDefaultErrorResponse(responses, "500", "Unexpected error")
      }
    }

    openApi.paths
      .forEach {
        val path: String = it.key

        val prefix = if (path.startsWith("/cas1/")) {
          "cas1-"
        } else if (path.startsWith("/cas2/")) {
          "cas2-"
        } else if (path.startsWith("/cas2v2/")) {
          "cas2v2-"
        } else if (path.startsWith("/cas3/")) {
          "cas3-"
        } else if (path.startsWith("/cas2-hdc/")) {
          "cas2-hdc-"
        } else {
          ""
        }

        val pathItem: PathItem = it.value
        if (pathItem.get != null) pathItem.get.operationId = prefix + pathItem.get.operationId
        if (pathItem.post != null) pathItem.post.operationId = prefix + pathItem.post.operationId
        if (pathItem.put != null) pathItem.put.operationId = prefix + pathItem.put.operationId
        if (pathItem.patch != null) pathItem.patch.operationId = prefix + pathItem.patch.operationId
        if (pathItem.delete != null) pathItem.delete.operationId = prefix + pathItem.delete.operationId
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
