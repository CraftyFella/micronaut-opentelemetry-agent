import de.undercouch.gradle.tasks.download.Download

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.kotlin.kapt") version "1.6.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.7.10"
}

version = "0.1"
group = "com.example"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(platform("io.opentelemetry:opentelemetry-bom:1.30.1"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.30.0")

    implementation("io.micronaut.mongodb:micronaut-mongo-reactive")
    implementation( group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core" )
    implementation( group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-reactor" )
}


application {
    mainClass.set("com.example.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("com.example.*")
    }
}

val javaAgentJar = "$buildDir/otel/opentelemetry-javaagent-all.jar"

// Task to download the OpenTelemetry agent
val downloadOpenTelemetryAgent by tasks.register<Download>("downloadOpenTelemetryAgent") {
    src("https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar")
    dest(javaAgentJar)
    onlyIfNewer(true)
}

val otelEndpoint = System.getenv("OTEL_ENDPOINT") ?: "http://localhost:4317"
val otelKey = System.getenv("OTEL_KEY") ?: "doesn't matter for local host"
val debug = System.getenv("OTEL_DEBUG") ?: "true"

val run by tasks.getting(JavaExec::class) {
    dependsOn(downloadOpenTelemetryAgent)
    jvmArgs = listOf(
        "-javaagent:$javaAgentJar",
        "-Dotel.service.name=dave",
        "-Dotel.traces.exporter=otlp",
        "-Dotel.metrics.exporter=otlp",
        "-Dotel.logs.exporter=otlp",
        "-Dotel.traces.sampler=always_on",
        "-Dotel.attribute.value.length.limit=4095",
        "-Dotel.exporter.otlp.compression=gzip",
        "-Dotel.exporter.otlp.headers=api-key=$otelKey",
        "-Dotel.exporter.otlp.endpoint=$otelEndpoint",
        "-Dotel.exporter.otlp.protocol=grpc",
        "-Dotel.exporter.otlp.timeout=30000",
        "-Dotel.javaagent.debug=$debug"
    )
}
//
//// Task to run Java with the specified commands
//val shadowRun by tasks.register<JavaExec>("shadowRun") {
//    dependsOn(task(":shadowJar"))
//    args("-javaagent:$javaAgentJar", "-jar", "${project.buildDir}/libs/${project.name}-${project.version}-all.jar")
//}
//
////val shadowRun2 by tasks.getting(JavaExec::class) {
////    dependsOn(downloadOpenTelemetryAgent)
////    dependsOn(task(":shadowJar"))
////    jvmArgs = listOf(
////        "-javaagent:$javaAgentJar=agent-arguments",
////        "-jar ${project.buildDir}/libs/${project.name}-${project.version}-all.jar",
////        "-Dotel.service.name=dave",
////        "-Dotel.traces.exporter=otlp",
////        "-Dotel.metrics.exporter=otlp",
////        "-Dotel.logs.exporter=otlp",
////        "-Dotel.traces.sampler=always_on",
////        "-Dotel.attribute.value.length.limit=4095",
////        "-Dotel.exporter.otlp.compression=gzip",
////        "-Dotel.exporter.otlp.headers=api-key=$otelKey",
////        "-Dotel.exporter.otlp.endpoint=$otelEndpoint",
////        "-Dotel.exporter.otlp.protocol=grpc",
////        "-Dotel.exporter.otlp.timeout=30000",
////        "-Dotel.javaagent.debug=$debug"
////    )
////}
//
