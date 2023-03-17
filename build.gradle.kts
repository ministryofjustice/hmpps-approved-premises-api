plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.2"
  kotlin("plugin.spring") version "1.8.10"
  id("org.openapi.generator") version "6.4.0"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.8.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val springDocVersion = "1.6.15"
val sentryVersion = "6.15.0"

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.2.0")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")

  runtimeOnly("org.postgresql:postgresql:42.5.4")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-data-rest:$springDocVersion")

  implementation("org.zalando:problem-spring-web-starter:0.27.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("io.sentry:sentry-spring-boot-starter:$sentryVersion")
  implementation("io.sentry:sentry-logback:$sentryVersion")

  implementation(kotlin("reflect"))

  implementation("com.networknt:json-schema-validator:1.0.78")

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")

  implementation("org.jetbrains.kotlinx:dataframe:0.9.1")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.13.4")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.0-beta-4")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }

  testImplementation("com.ninja-squad:springmockk:4.0.2")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.0-beta-13")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }

    kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated/src/main")
    dependsOn("openApiGenerate")
  }
}

tasks.register("bootRunLocal") {
  group = "application"
  description = "Runs this project as a Spring Boot application with the local profile"
  doFirst {
    tasks.bootRun.configure {
      systemProperty("spring.profiles.active", "local")
    }
  }
  finalizedBy("bootRun")
}

tasks.test {
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("useSpringBoot3", "true")
    put("annotationLibrary", "none")
    put("documentationProvider", "none")
  }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateDomainEvents") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/domain-events-api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("useSpringBoot3", "true")
    put("annotationLibrary", "none")
    put("documentationProvider", "none")
  }
}

tasks.get("openApiGenerate").dependsOn("openApiGenerateDomainEvents")

tasks.get("openApiGenerate").doLast {
  // This is a workaround to allow us to have the `/documents/{crn}/{documentId}` endpoint specified in api.yml but not use
  // OpenAPI Generate for the scaffolding.  This is because we need to properly stream the files
  // (rather than loading whole file into memory first) which the OpenAPI generated controller does not support.

  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApi.kt").delete()
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApiController.kt").delete()
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/DocumentsApiDelegate.kt").delete()
}

ktlint {
  filter {
    exclude { it.file.path.contains("$buildDir${File.separator}generated${File.separator}") }
  }
}

allOpen {
  annotations("jakarta.persistence.Entity")
}
