plugins {
	kotlin("jvm") version "2.3.10"
	application
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

// Override with -PspringAiVersion=1.1.8 to check a released line instead of main.
val springAiVersion: String = (findProperty("springAiVersion") as String?) ?: "2.0.1-SNAPSHOT"

dependencies {
	implementation("org.springframework.ai:spring-ai-model:$springAiVersion")
	implementation("org.springframework.ai:spring-ai-mcp-annotations:$springAiVersion")
	implementation(kotlin("reflect"))

	testImplementation(kotlin("test"))
	testImplementation(platform("org.junit:junit-bom:5.11.4"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	jvmToolchain(17)
	compilerOptions {
		// Mirrors a typical Spring Boot Kotlin build so parameter names are retained.
		freeCompilerArgs.add("-java-parameters")
	}
}

application {
	mainClass.set("repro.ReproKt")
}

tasks.test {
	useJUnitPlatform()
	testLogging { showStandardStreams = true }
}
