import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mainClass = "no.nav.modiapersonoversikt.MainKt"
val kotlinVersion = "2.0.10"
val ktorVersion = "2.3.12"
val prometheusVersion = "1.13.2"
val logbackVersion = "1.5.6"
val logstashVersion = "8.0"
val modiaCommonVersion = "1.2024.07.29-11.54-9ab053425c41"
val flywayVersion = "10.17.0"

plugins {
    kotlin("jvm") version "2.0.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    idea
}

repositories {
    mavenCentral()
    mavenLocal()

    val githubToken = System.getenv("GITHUB_TOKEN")
    if (githubToken.isNullOrEmpty()) {
        maven {
            name = "external-mirror-github-navikt"
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
    } else {
        maven {
            name = "github-package-registry-navikt"
            url = uri("https://maven.pkg.github.com/navikt/maven-release")
            credentials {
                username = "token"
                password = githubToken
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")

    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")

    implementation("com.github.navikt.modia-common-utils:ktor-utils:$modiaCommonVersion")
    implementation("com.github.navikt.modia-common-utils:kotlin-utils:$modiaCommonVersion")
    implementation("com.github.navikt.modia-common-utils:crypto:$modiaCommonVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    implementation("no.nav:vault-jdbc:1.3.10")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.github.seratch:kotliquery:1.9.0")

    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks {
    shadowJar {
        mergeServiceFiles {
            setPath("META-INF/services/org.flywaydb.core.extensibility.Plugin")
        }
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClass,
                ),
            )
        }
    }
    "build" {
        dependsOn("shadowJar")
    }
}
