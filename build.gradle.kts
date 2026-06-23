plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.5.3"
  kotlin("plugin.spring") version "2.4.0"
  kotlin("plugin.jpa") version "2.4.0"
  id("dev.detekt") version "2.0.0-alpha.5"
}

kotlin {
  jvmToolchain(25)
}

// detekt must use a specific kotlin version when running, this block ensures it's using the correct version
// this is variation on https://detekt.dev/docs/gettingstarted/gradle/#gradle-runtime-dependencies
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(dev.detekt.gradle.plugin.getSupportedKotlinVersion())
    }
  }
}

dependencies {
  val hmppsSpringBootStarterVersion = "2.5.0"
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsSpringBootStarterVersion")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.retry:spring-retry")
  implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.15.3")
  implementation("org.hibernate.orm:hibernate-spatial")
  implementation("org.hibernate.orm:hibernate-jcache")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.google.guava:guava:33.6.0-jre")
  implementation("org.javers:javers-core:7.11.4")

  val springDocOpenApiStarterVersion = "3.0.2"
  // https://github.com/springdoc/springdoc-openapi/pull/3256 significantly changed our
  // generated schema, making it incompatible with the typescript generators and in some
  // places it was incorrect. We're pinning version 3.0.2 until a new version is available
  // reverting this change, as proposed by https://github.com/springdoc/springdoc-openapi/pull/3276
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiStarterVersion")
  // this is a transitive dependency of uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-autoconfigure
  // so we need to force a different version
  implementation("org.springdoc:springdoc-openapi-starter-common:$springDocOpenApiStarterVersion")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("io.sentry:sentry-spring-boot-4:8.43.2")

  runtimeOnly("org.postgresql:postgresql:42.7.11")
  runtimeOnly("org.ehcache:ehcache")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  implementation(kotlin("reflect"))

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.10.0")

  implementation("org.jetbrains.kotlinx:dataframe:0.15.0") {
    exclude(group = "org.jetbrains.kotlinx", module = "dataframe-openapi")
    exclude(group = "org.jetbrains.kotlinx", module = "dataframe-arrow")
    exclude(group = "org.jetbrains.kotlinx", module = "dataframe-jdbc")
  }
  implementation("org.apache.poi:poi-ooxml:5.5.1")

  implementation("io.arrow-kt:arrow-core:2.2.3")

  implementation("com.opencsv:opencsv:5.12.0")

  val shedLockVersion = "7.7.0"
  implementation("net.javacrumbs.shedlock:shedlock-spring:$shedLockVersion")
  implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:$shedLockVersion")

  implementation("org.jetbrains.kotlinx:dataframe-excel:0.15.0")

  val appinsightsCore = "core:2.6.4"
  implementation("io.micrometer:micrometer-registry-azure-monitor:1.17.0")
  implementation("com.microsoft.azure:applicationinsights-$appinsightsCore")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.0")
  implementation("uk.gov.service.notify:notifications-java-client:6.0.0-RELEASE")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.14.11")
  testImplementation("org.wiremock.integrations:wiremock-spring-boot:4.2.1")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsSpringBootStarterVersion")

  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  testImplementation("org.zalando:logbook-spring-boot-starter:4.0.4")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.45") {
    exclude(group = "io.swagger.core.v3")
  }

  testImplementation("org.awaitility:awaitility:4.3.0")
}

springBoot {
  mainClass.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.ApplicationKt")
}

kotlin.compilerOptions.freeCompilerArgs.add("-Xannotation-default-target=param-property")

// The `buildDir` built-in property has been deprecated in favour of `layout.buildDirectory`
// This is used in multiple places, so for convenience `buildDir` is redefined here.
val buildDir = layout.buildDirectory.asFile.get()

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    finalizedBy("generateCas1Roles")
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

  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath

  useJUnitPlatform {
    includeTags("integration")
  }

  reports {
    junitXml.outputLocation.set(file("$buildDir/test-results/integrationTest"))
  }
}

tasks.register<Test>("unitTest") {
  group = "verification"
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath

  useJUnitPlatform {
    excludeTags("integration")
  }

  reports {
    junitXml.outputLocation.set(file("$buildDir/test-results/unitTest"))
  }
}
tasks.register<JavaExec>("generateCas1Roles") {
  group = "build"
  description = "Generates the built-cas1-roles.json file using Cas1RolesFileGenerator."

  mainClass.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserRoleJsonFileGenerator")

  classpath = sourceSets["main"].runtimeClasspath

  outputs.file(file("built-cas1-roles.json"))
}

allOpen {
  annotations("jakarta.persistence.Entity")
}

tasks {
  withType<JavaExec> {
    jvmArgs.plus("--add-opens")
    jvmArgs.plus("java.base/java.lang=ALL-UNNAMED")
  }
}

tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("assemble")

detekt {
  config.setFrom("./detekt.yml")
  buildUponDefaultConfig = true
  ignoreFailures = false
  baseline = file("./detekt-baseline.xml")
}

dependencyCheck {
  suppressionFiles.add(".cas-api-dependency-check-ignore.xml")
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
  source = source.asFileTree.matching {
    exclude("**/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/**")
  }
}

tasks.withType<dev.detekt.gradle.DetektCreateBaselineTask>().configureEach {
  source = source.asFileTree.matching {
    exclude("**/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/**")
  }
}

tasks.register("printRuntimeClasspath") {
  println(sourceSets.main.get().runtimeClasspath.asPath)
}
