import org.apache.commons.io.FileUtils

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.2"
  kotlin("plugin.spring") version "2.0.21"
  id("org.openapi.generator") version "7.11.0"
  id("org.jetbrains.kotlin.plugin.jpa") version "1.9.22"
  id("io.gatling.gradle") version "3.13.1"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.retry:spring-retry")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")
  implementation("org.hibernate:hibernate-spatial:6.6.4.Final")
  implementation("org.hibernate.orm:hibernate-jcache")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.google.guava:guava:33.3.1-jre")
  implementation("org.postgresql:postgresql:42.7.4")
  implementation("org.javers:javers-core:7.7.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

  implementation("org.zalando:problem-spring-web-starter:0.29.1")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.11.0")

  runtimeOnly("org.ehcache:ehcache")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  implementation(kotlin("reflect"))

  implementation("com.networknt:json-schema-validator:1.1.0")

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")

  implementation("org.jetbrains.kotlinx:dataframe:0.13.1") {
    exclude(group = "org.jetbrains.kotlinx", module = "dataframe-openapi")
  }

  implementation("io.arrow-kt:arrow-core:1.2.4")
  implementation("io.github.s-sathish:redlock-java:1.0.4")

  implementation("com.opencsv:opencsv:5.9")

  implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.16.0")
  implementation("org.jetbrains.kotlinx:dataframe-excel:0.13.1")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.13.13")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }

  testImplementation("com.ninja-squad:springmockk:4.0.2")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.3")

  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")

  gatlingImplementation("org.springframework.boot:spring-boot-starter-webflux")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The `buildDir` built-in property has been deprecated in favour of `layout.buildDirectory`
// This is used in multiple places, so for convenience `buildDir` is redefined here.
val buildDir = layout.buildDirectory.asFile.get()

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }

    dependsOn("openApiGenerate")
    getByName("check") {
      dependsOn(":ktlintCheck", "detekt")
    }
  }

  compileJava { enabled = false }
  compileTestJava { enabled = false }
}

// set the generated set globally
sourceSets {
  main {
    kotlin {
      srcDirs("src/main/kotlin", "$buildDir/generated/src/main/kotlin")
    }
  }
}

// this is deprecated in favour of bootRunDebug, which does not set an active profile
// it will be removed once ap-tools has been updated to use bootRunDebug
tasks.register("bootRunLocal") {
  group = "application"
  description = "Runs this project as a Spring Boot application with the local profile"
  doFirst {
    tasks.bootRun.configure {
      systemProperty("spring.profiles.active", "local")
      jvmArgs("-Xmx512m", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=32323")
    }
  }
  finalizedBy("bootRun")
}

tasks.register("bootRunDebug") {
  group = "application"
  description = "Runs this project as a Spring Boot application with debug configuration"
  doFirst {
    tasks.bootRun.configure {
      jvmArgs("-Xmx512m", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=32323")
    }
  }
  finalizedBy("bootRun")
}

tasks.bootRun {
  System.getenv()["BOOT_RUN_ENV_FILE"]?.let { envFilePath ->
    println("Reading env vars from file $envFilePath")
    file(envFilePath).readLines().forEach {
      if (it.isNotBlank() && !it.startsWith("#")) {
        val (key, value) = it.split("=", limit = 2)
        println("Setting env var $key")
        environment(key, value)
      }
    }
  }
}

tasks.withType<Test> {
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", "--add-opens", "java.base/java.time=ALL-UNNAMED")

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

// Skip OpenAPI generation if running tests from intellij
val entryPointTask = project.gradle.startParameter.taskNames.firstOrNull()?.let {
  project.tasks.getByName(it.replace(":", ""))
}
val isTestInvokedFromIntellij = (entryPointTask is Test && System.getProperty("idea.active") !== null)
if (isTestInvokedFromIntellij) {
  tasks.withType<org.openapitools.generator.gradle.plugin.tasks.GenerateTask> {
    enabled = false
  }
}

fun addOpenApiConfigOptions(
  configOptions: MapProperty<String, String>,
  useTags: Boolean = false,
) {
  configOptions.apply {
    put("basePackage", "uk.gov.justice.digital.hmpps.approvedpremisesapi")
    put("delegatePattern", "true")
    put("gradleBuildFile", "false")
    put("exceptionHandler", "false")
    put("useBeanValidation", "false")
    put(
      "useTags",
      if (useTags) {
        "true"
      } else {
        "false"
      },
    )

    put("dateLibrary", "custom")
    put("enumPropertyNaming", "camelCase")
  }
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set("$rootDir/src/main/resources/static/codegen/built-api-spec.yml")
  outputDir.set("$buildDir/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model")

  addOpenApiConfigOptions(configOptions)
  typeMappings.put("DateTime", "Instant")
  importMappings.put("Instant", "java.time.Instant")
  templateDir.set("$rootDir/openapi")
  additionalProperties.put("removeEnumValuePrefix", "true")

  generateModelTests.set(false) // Optional: Disable model test generation
  generateApiTests.set(false) // Optional: Disable API test generation
  generateApiDocumentation.set(false) // Optional: Disable API docs

  // Enable only model generation
  configOptions.set(
    mapOf(
      "api" to "false", // Disable API generation
      "models" to "true", // Enable models
      "supportingFiles" to "false", // Disable supporting files
    ),
  )
}

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas1Namespace",
  ymlPath = "$rootDir/src/main/resources/static/codegen/built-cas1-api-spec.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model",
  useTags = true,
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas2v2Namespace",
  ymlPath = "$rootDir/src/main/resources/static/codegen/built-cas2v2-api-spec.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model",
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas2Namespace",
  ymlPath = "$rootDir/src/main/resources/static/codegen/built-cas2-api-spec.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model",
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas3Namespace",
  ymlPath = "$rootDir/src/main/resources/static/codegen/built-cas3-api-spec.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model",
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateDomainEvents",
  ymlPath = "$rootDir/src/main/resources/static/domain-events-api.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model",
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas2DomainEvents",
  ymlPath = "$rootDir/src/main/resources/static/cas2-domain-events-api.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model",
  useTags = true,
)

registerAdditionalOpenApiGenerateTask(
  name = "openApiGenerateCas3DomainEvents",
  ymlPath = "$rootDir/src/main/resources/static/cas3-domain-events-api.yml",
  modelPackageName = "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model",
  useTags = true,
)

fun registerAdditionalOpenApiGenerateTask(
  name: String,
  ymlPath: String,
  modelPackageName: String,
  useTags: Boolean = false,
) {
  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>(name) {
    generatorName.set("kotlin-spring")
    inputSpec.set(ymlPath)
    outputDir.set("$buildDir/generated")
    modelPackage.set(modelPackageName)
    addOpenApiConfigOptions(configOptions, useTags)
    typeMappings.put("DateTime", "Instant")
    importMappings.put("Instant", "java.time.Instant")
    templateDir.set("$rootDir/openapi")
  }
}

tasks.register("openApiPreCompilation") {

  fun rewriteRefsForLocalComponents(file: File) {
    val updatedContents = FileUtils
      .readFileToString(file, "UTF-8")
      .replace("_shared.yml#/components", "#/components")
      .replace("cas1-schemas.yml#/components", "#/components")
      .replace("cas2v2-schemas.yml#/components", "#/components")
    FileUtils.writeStringToFile(file, updatedContents, "UTF-8")
  }

  tasks.get("openApiGenerate").dependsOn(
    "openApiGenerateDomainEvents",
    "openApiGenerateCas3DomainEvents",
    "openApiGenerateCas2DomainEvents",
    "openApiPreCompilation",
    "openApiGenerateCas1Namespace",
    "openApiGenerateCas2Namespace",
    "openApiGenerateCas2v2Namespace",
    "openApiGenerateCas3Namespace",
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
      exclude {
        it.file.path.contains("$buildDir${File.separator}generated${File.separator}") ||
          it.file.path.contains("controller${File.separator}generated${File.separator}")
      }
    }
  }

  allOpen {
    annotations("jakarta.persistence.Entity")
  }

  tasks {
    withType<JavaExec> {
      jvmArgs!!.plus("--add-opens")
      jvmArgs!!.plus("java.base/java.lang=ALL-UNNAMED")
    }
  }

  tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("openApiGenerate", "openApiGenerateDomainEvents")

  gatling {
    gatlingVersion = "3.12.0"
  }

  detekt {
    config = files("./detekt.yml")
    buildUponDefaultConfig = true
    ignoreFailures = false
    baseline = file("./detekt-baseline.xml")
  }

  tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    source = source.asFileTree.matching {
      exclude("**/uk/gov/justice/digital/hmpps/approvedpremisesapi/controller/generated/**")
    }
  }
}
