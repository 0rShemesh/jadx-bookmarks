plugins {
	java
}

group = "io.github.skylot"
version = "1.0.0"

repositories {
	mavenCentral()
	google()
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
	compileOnly("io.github.skylot:jadx-core:1.5.2")
	compileOnly("io.github.skylot:jadx-gui:1.5.2")
	compileOnly("org.slf4j:slf4j-api:2.0.17")
	compileOnly("org.jetbrains:annotations:26.0.2")
	compileOnly("com.google.code.gson:gson:2.10.1")
	compileOnly("com.fifesoft:rsyntaxtextarea:3.4.1")

	testImplementation("io.github.skylot:jadx-core:1.5.2")
	testImplementation("io.github.skylot:jadx-gui:1.5.2")
	testImplementation("com.google.code.gson:gson:2.10.1")
	testImplementation("org.assertj:assertj-core:3.25.3")
	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}

tasks {
	compileJava {
		options.encoding = "UTF-8"
	}

	jar {
		manifest {
			attributes(
				"Plugin-Id" to "jadx-bookmarks",
				"Plugin-Name" to "JADX Bookmarks",
				"Plugin-Version" to version
			)
		}
	}
}
