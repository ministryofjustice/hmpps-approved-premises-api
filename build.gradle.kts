import org.apache.commons.io.FileUtils

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.14.0"
  kotlin("plugin.spring") version "1.9.22"
  id("org.openapi.generator") version "5.4.0"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.9.22"
  id("io.gatling.gradle") version "3.10.3.2"
  id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.9.21")
    }
  }
}

val springDocVersion = "1.7.0"
val sentryVersion = "7.3.0"

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.retry:spring-retry")
  implementation("com.vladmihalcea:hibernate-types-55:2.21.1")
  implementation("org.locationtech.jts:jts-core:1.19.0")
  implementation("org.hibernate:hibernate-spatial")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.google.guava:guava:33.0.0-jre")

  runtimeOnly("org.postgresql:postgresql:42.7.1")

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

  implementation("com.networknt:json-schema-validator:1.1.0")

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")

  implementation("org.jetbrains.kotlinx:dataframe:0.12.1")

  implementation("io.arrow-kt:arrow-core:1.2.1")
  implementation("io.github.s-sathish:redlock-java:1.0.4")

  implementation("io.flipt:flipt-java:1.1.0") {
    exclude("org.apache.httpcomponents", "httpclient")
  }

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }

  testImplementation("com.ninja-squad:springmockk:4.0.2")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.3.1")

  implementation("uk.gov.service.notify:notifications-java-client:5.0.0-RELEASE")

  gatlingImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// The `buildDir` built-in property has been deprecated in favour of `layout.buildDirectory`
// This is used in multiple places, so for convenience `buildDir` is redefined here.
val buildDir = layout.buildDirectory.asFile.get()

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }

    kotlin.sourceSets["main"].kotlin.srcDir("$buildDir/generated/src/main")
    dependsOn("openApiGenerate")
    getByName("check") {
      dependsOn(":ktlintCheck", "detekt")
    }
  }
}

tasks.register("bootRunLocal") {
  group = "application"
  description = "Runs this project as a Spring Boot application with the local profile"
  doFirst {
    tasks.bootRun.configure {
      systemProperty("spring.profiles.active", "local")
      jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=32323")
    }
  }
  finalizedBy("bootRun")
}

tasks.withType<Test> {
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.time=ALL-UNNAMED")

  afterEvaluate {
    if (environment["CI"] != null) {
      maxParallelForks = Runtime.getRuntime().availableProcessors()
      println("Running in CI - setting max test processes to number of processors: $maxParallelForks")
    } else {
      maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
      println("Setting max test processes to recommended half of available: $maxParallelForks")
    }
  }
}

tasks.register<Test>("integrationTest") {
  group = "verification"

  useJUnitPlatform {
    includeTags("integration")
  }
}

tasks.register<Test>("unitTest") {
  group = "verification"

  useJUnitPlatform {
    excludeTags("integration")
  }
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/codegen/built-api-spec.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
  templateDir.set("$rootDir/openapi")
}

// Skip OpenAPI generation for test tasks run inside IntelliJ
tasks.withType<org.openapitools.generator.gradle.plugin.tasks.GenerateTask> {
  onlyIf {
    val currentTask = project.tasks.getByName(project.gradle.startParameter.taskNames.first().replace(":", ""))

    !(currentTask is Test && System.getProperty("idea.active") !== null)
  }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCas1Namespace") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/codegen/built-cas1-api-spec.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("apiSuffix", "Cas1")
    put("dateLibrary", "custom")
    put("useTags", "true")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
  templateDir.set("$rootDir/openapi")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCas2Namespace") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/codegen/built-cas2-api-spec.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("apiSuffix", "Cas2")
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
  templateDir.set("$rootDir/openapi")
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
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCas3DomainEvents") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/cas3-domain-events-api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("dateLibrary", "custom")
    put("useTags", "true")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCas2DomainEvents") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/cas2-domain-events-api.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("dateLibrary", "custom")
    put("useTags", "true")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCas3Namespace") {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/codegen/built-cas3-api-spec.yml")
  outputDir.set("$buildDir/generated")
  apiPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put("apiSuffix", "Cas3")
    put("dateLibrary", "custom")
  }
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
  templateDir.set("$rootDir/openapi")
}

tasks.register("openApiPreCompilation") {

  // Generate OpenAPI spec files suited to Kotlin code generator
  // -----------------------------------------------------------
  // The 'built' files produced each contain all the shared 'components'
  // -- as the Kotlin generator doesn't support $ref links to 'remote' files.

  logger.quiet("Running task: openApiPreCompilation")

  val sharedComponents = FileUtils.readFileToString(
    File("$rootDir/src/main/resources/static/_shared.yml"),
    "UTF-8"
  )

  fun buildSpecWithSharedComponentsAppended(specName: String): File {
    val spec = FileUtils.readFileToString(
      File("$rootDir/src/main/resources/static/$specName.yml"),
      "UTF-8"
    )
    val compiledSpecFile = File("$rootDir/src/main/resources/static/codegen/built-$specName-spec.yml")
    val notice = "# DO NOT EDIT.\n# This is a build artefact for use in code generation.\n"

    FileUtils.writeStringToFile(
      compiledSpecFile,
      (notice + spec + sharedComponents),
      "UTF-8"
    )

    return compiledSpecFile
  }

  fun rewriteRefsForLocalComponents(file: File) {
    val updatedContents = FileUtils
      .readFileToString(file, "UTF-8")
      .replace("_shared.yml#/components", "#/components")
    FileUtils.writeStringToFile(file, updatedContents, "UTF-8")
  }

  listOf("api", "cas1-api", "cas2-api", "cas3-api").forEach {
    buildSpecWithSharedComponentsAppended(it)
      .run(::rewriteRefsForLocalComponents)
  }
}

tasks.get("openApiGenerate").dependsOn(
  "openApiGenerateDomainEvents",
  "openApiGenerateCas3DomainEvents",
  "openApiGenerateCas2DomainEvents",
  "openApiPreCompilation",
  "openApiGenerateCas1Namespace",
  "openApiGenerateCas2Namespace",
  "openApiGenerateCas3Namespace"
)

tasks.get("openApiGenerate").doLast {
  // This is a workaround for an issue where we end up with duplicate keys in output JSON because we declare properties both in the discriminator
  // and as a regular property in the OpenAPI spec.  The Typescript generator does not support just the discriminator so there is no alternative.
  File("$rootDir/build/generated/src/main/kotlin/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/model").walk()
    .forEach {
      if (it.isFile && it.extension == "kt") {
        val replacedFileContents = FileUtils.readFileToString(it, "UTF-8")
          .replace("include = JsonTypeInfo.As.PROPERTY", "include = JsonTypeInfo.As.EXISTING_PROPERTY")
        FileUtils.writeStringToFile(it, replacedFileContents, "UTF-8")
      }
    }
}

ktlint {
  filter {
    exclude { it.file.path.contains("$buildDir${File.separator}generated${File.separator}") }
  }
}

allOpen {
  annotations("javax.persistence.Entity")
}

tasks {
  withType<JavaExec> {
    jvmArgs!!.plus("--add-opens")
    jvmArgs!!.plus("java.base/java.lang=ALL-UNNAMED")
  }
}

tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("openApiGenerate", "openApiGenerateDomainEvents")

gatling {
  gatlingVersion = "3.9.5"
  // WARNING: options below only work when logback config file isn't provided
  logLevel = "WARN" // logback root level
  logHttp =
    io.gatling.gradle.LogHttp.NONE // set to 'ALL' for all HTTP traffic in TRACE, 'FAILURES' for failed HTTP traffic in DEBUG
}

detekt {
  config = files("./detekt.yml")
  buildUponDefaultConfig = true
  ignoreFailures = false
  baseline = file("./detekt-baseline.xml")
}
