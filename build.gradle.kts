import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

group = "no.nav.syfo"
version = "1.0-SNAPSHOT"

object Versions {
    const val coroutinesVersion = "1.7.3"
    const val kotlinSerializationVersion = "0.20.0"
    const val ktorVersion = "2.3.2"
    const val logbackVersion = "1.2.3"
    const val logstashEncoderVersion = "5.1"
    const val prometheusVersion = "0.8.1"
    const val jacksonVersion = "2.15.2"
    const val mockkVersion = "1.13.5"
    const val kotestVersion = "5.6.2"
    const val kotestExtensionsVersion = "2.0.0"
    const val kotlinVersion = "1.9.0"
    const val javaJwtVersion = "4.4.0"
    const val nimbusVersion = "9.31"
    const val detektVersion = "1.23.1"
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "no.nav.syfo.MainApplicationKt"
}

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.kotlinSerializationVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:${Versions.kotlinSerializationVersion}")

    implementation("io.prometheus:simpleclient_hotspot:${Versions.prometheusVersion}")
    implementation("io.prometheus:simpleclient_common:${Versions.prometheusVersion}")

    implementation("io.ktor:ktor-server-netty:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-server-status-pages:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-server-call-id:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-serialization-jackson:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-apache:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-logging:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-logging-jvm:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-jackson:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-core:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktorVersion}")

    implementation("ch.qos.logback:logback-classic:${Versions.logbackVersion}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoderVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jacksonVersion}")

    // Auth
    implementation("io.ktor:ktor-server-auth:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-server-auth-jwt:${Versions.ktorVersion}")
    implementation("com.auth0:java-jwt:${Versions.javaJwtVersion}")
    implementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusVersion}")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlinVersion}")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:${Versions.kotestVersion}")
    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotestVersion}")
    testImplementation("io.kotest:kotest-property:${Versions.kotestVersion}")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:${Versions.kotestExtensionsVersion}")
    testImplementation("io.mockk:mockk:${Versions.mockkVersion}")
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktorVersion}")

    api("io.ktor:ktor-client-mock:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-mock-jvm:${Versions.ktorVersion}")

    constraints {
        implementation("org.eclipse.jetty:jetty-io:11.0.2")
        implementation("io.netty:netty-codec:4.1.106.Final")
        implementation("net.minidev:json-smart:1.3.2")
    }
}

detekt {
    toolVersion = Versions.detektVersion
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}


java.toolchain {
    languageVersion.set(JavaLanguageVersion.of(19))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}

tasks {
    create("printVersion") {
        println(project.version)
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}
