plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.4"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.owasp.dependencycheck") version "12.1.3"
}

kotlin {
  jvmToolchain(21)
}

configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
    }
  }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.11")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.retry:spring-retry")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")
  // this should match the version of hibernate provided by spring
  implementation("org.hibernate:hibernate-spatial:6.6.4.Final")
  implementation("org.hibernate.orm:hibernate-jcache")
  implementation("org.flywaydb:flyway-core")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("com.google.guava:guava:33.4.8-jre")
  implementation("org.postgresql:postgresql:42.7.7")
  implementation("org.javers:javers-core:7.8.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

  implementation("org.zalando:problem-spring-web-starter:0.29.1")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.17.0")

  runtimeOnly("org.ehcache:ehcache")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  implementation(kotlin("reflect"))

  implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.10.0")

  implementation("org.jetbrains.kotlinx:dataframe:0.15.0") {
    exclude(group = "org.jetbrains.kotlinx", module = "dataframe-openapi")
  }
  implementation("org.apache.poi:poi-ooxml:5.3.0")

  implementation("io.arrow-kt:arrow-core:2.1.2")

  implementation("com.opencsv:opencsv:5.12.0")

  implementation("net.javacrumbs.shedlock:shedlock-spring:6.9.2")
  implementation("net.javacrumbs.shedlock:shedlock-provider-redis-spring:6.9.2")
  implementation("org.jetbrains.kotlinx:dataframe-excel:0.15.0")

  testImplementation("io.github.bluegroundltd:kfactory:1.0.0")
  testImplementation("io.mockk:mockk:1.14.5")
  testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
  testImplementation("io.jsonwebtoken:jjwt-api:0.12.6")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(module = "mockito-core")
  }

  testImplementation("com.ninja-squad:springmockk:4.0.2")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")

  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")
}

springBoot {
  mainClass.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.ApplicationKt")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The `buildDir` built-in property has been deprecated in favour of `layout.buildDirectory`
// This is used in multiple places, so for convenience `buildDir` is redefined here.
val buildDir = layout.buildDirectory.asFile.get()

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
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
tasks.register<JavaExec>("generateCas1Roles") {
  group = "build"
  description = "Generates the built-cas1-roles.json file using Cas1RolesFileGenerator."

  mainClass.set("uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserRoleJsonFileGenerator")

  classpath = sourceSets["main"].runtimeClasspath

  outputs.file(file("built-cas1-roles.json"))
}

tasks.named("assemble") {
  finalizedBy("generateCas1Roles")
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

tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("assemble")

detekt {
  config.setFrom("./detekt.yml")
  buildUponDefaultConfig = true
  ignoreFailures = false
  baseline = file("./detekt-baseline.xml")
}

dependencyCheck {
  suppressionFile = ".dependencycheckignore.xml"
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  source = source.asFileTree.matching {
    exclude("**/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/**")
  }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
  source = source.asFileTree.matching {
    exclude("**/uk/gov/justice/digital/hmpps/approvedpremisesapi/api/**")
  }
}
