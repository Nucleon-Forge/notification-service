import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.gorylenko.gradle-git-properties") version "2.5.3"
    id("com.diffplug.spotless") version "8.0.0"
}

group = "com.sivalabs.ft"
version = "0.0.2-SNAPSHOT"

val dockerImageName: String =
    project
        .findProperty("dockerImageName")
        ?.let { it as String }
        ?: "sivaprasadreddy/ft-notification-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven {
        name = "Nucleon Forge Axile"
        url = uri("https://maven.pkg.github.com/Nucleon-Forge/axile")
        credentials {
            username = System.getenv("USERNAME") ?: project.properties["USERNAME"].toString()
            password = System.getenv("PERSONAL_ACCESS_TOKEN") ?: project.properties["PERSONAL_ACCESS_TOKEN"].toString()
        }
        mavenContent {
            snapshotsOnly()
        }
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

extra["springCloudVersion"] = "2025.0.0"

dependencies {
    implementation("com.nucleonforge.axile:auto-configuration:1.0.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    // implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.kafka:spring-kafka")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events(FAILED, STANDARD_ERROR, SKIPPED)
        exceptionFormat = FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<BootBuildImage> {
    imageName.set(dockerImageName)
}

gitProperties {
    failOnNoGitDirectory = false
    keys =
        listOf(
            "git.branch",
            "git.commit.id.abbrev",
            "git.commit.user.name",
            "git.commit.message.full",
        )
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
        ktlint("1.4.1")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.4.1")
    }
}
