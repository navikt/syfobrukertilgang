import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0-SNAPSHOT"

object Versions {
    const val coroutinesVersion = "1.5.2"
    const val kluentVersion = "1.59"
    const val kotlinSerializationVersion = "0.20.0"
    const val ktorVersion = "1.6.0"
    const val logbackVersion = "1.2.3"
    const val logstashEncoderVersion = "5.1"
    const val prometheusVersion = "0.8.1"
    const val spekVersion = "2.0.9"
    const val jacksonVersion = "2.9.8"
    const val mockkVersion = "1.10.0"
    const val nimbusdsVersion = "9.22"
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "no.nav.syfo.MainApplicationKt"
}

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.diffplug.gradle.spotless") version "3.18.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
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
    implementation("io.ktor:ktor-client-cio:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-apache:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-auth-basic-jvm:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-logging:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-logging-jvm:${Versions.ktorVersion}")

    implementation("io.ktor:ktor-jackson:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-client-jackson:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-auth:${Versions.ktorVersion}")
    implementation("io.ktor:ktor-auth-jwt:${Versions.ktorVersion}")

    implementation("ch.qos.logback:logback-classic:${Versions.logbackVersion}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoderVersion}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${Versions.jacksonVersion}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jacksonVersion}")

    testImplementation("org.amshove.kluent:kluent:${Versions.kluentVersion}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spekVersion}")
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktorVersion}")
    testImplementation("io.mockk:mockk:${Versions.mockkVersion}")
    testImplementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusdsVersion}")
    testRuntimeOnly("org.spekframework.spek2:spek-runtime-jvm:${Versions.spekVersion}")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spekVersion}")

    api("io.ktor:ktor-client-mock:${Versions.ktorVersion}")
    api("io.ktor:ktor-client-mock-jvm:${Versions.ktorVersion}")

    constraints {
        implementation("org.eclipse.jetty:jetty-io:11.0.2")
        implementation("io.netty:netty-codec:4.1.73.Final")
        implementation("net.minidev:json-smart:1.3.2")
        implementation("com.nimbusds:nimbus-jose-jwt:7.8.1")
    }
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

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
