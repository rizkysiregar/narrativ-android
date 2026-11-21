import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/*
 * Build types: debug, release
 * Product flavors (dimension "env"): local (emulator backend), dev, prod
 */


plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
	alias(libs.plugins.hilt.plugin)
	alias(libs.plugins.ksp)
	alias(libs.plugins.google.services)
	alias(libs.plugins.firebase.crashlytics)
	alias(libs.plugins.firebase.perf)
	alias(libs.plugins.kover)
}

// Release signing comes from keystore.properties (see keystore.properties.example) or, on CI, from
// the KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD environment variables.
// Neither is committed. Without them the release build is simply left unsigned.
val keystoreProperties = Properties().apply {
	val file = rootProject.file("keystore.properties")
	if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(propertyName: String, envName: String): String? =
	keystoreProperties.getProperty(propertyName) ?: System.getenv(envName)

// Coverage is measured on logic, not on generated code or Compose layout.
kover {
	reports {
		filters {
			excludes {
				classes(
					"*.BuildConfig",
					"*.R",
					"*.R$*",
					"*ComposableSingletons*",
					"*Hilt_*",
					"*_HiltModules*",
					"*_Factory",
					"*_Factory$*",
					"*_MembersInjector",
					"*_GeneratedInjector",
					"dagger.hilt.*",
					"hilt_aggregated_deps.*",
					"com.rizkysiregar.narrativ.di.*",
					"com.rizkysiregar.narrativ.ui.theme.*",
					"com.rizkysiregar.narrativ.ui.components.*",
					"com.rizkysiregar.narrativ.data.remote.model.*",
					"*ScreenKt*",
					"*RouteKt*",
					"*ViewKt*",
					"*_AssistedFactory*",
					// thin adapters over an SDK that cannot run in a JVM test; the logic around them is tested
					// through their interfaces (PushTokenProvider, GoogleIdTokenProvider, MediaInspector, ...)
					"com.rizkysiregar.narrativ.data.session.CredentialManagerGoogleIdTokenProvider",
					"com.rizkysiregar.narrativ.data.session.FirebasePushTokenProvider",
					"com.rizkysiregar.narrativ.domain.post.AndroidMediaInspector",
					"com.rizkysiregar.narrativ.telemetry.Firebase*",
					"com.rizkysiregar.narrativ.config.FirebaseRemoteConfigManager*"
				)
			}
		}

	}
}

// A floor per area, a little under where it is today, so coverage there only ever goes up. Kover's own verify rules
// cannot filter per rule, so this reads its XML report. What matters is the risk (see docs/TEST_PLAN.md), not the total:
// the total is only a net.
val coverageFloors = listOf(
	Triple("network and background work", listOf("data/remote"), 90),
	Triple("session", listOf("data/session"), 90),
	Triple("repositories", listOf("data/repository"), 85),
	Triple("auth view models", listOf("ui/feature/auth"), 85),
	Triple("settings and post composer view models", listOf("ui/feature/home/settings", "ui/feature/home/post"), 85),
	Triple("media preparation and upload queue", listOf("domain/post"), 90),
	Triple("profile photo", listOf("domain/profile"), 85),
	Triple("util", listOf("util"), 75),
	Triple("total", listOf(""), 35)
)

tasks.register("verifyCoverageFloors") {
	group = "verification"
	description = "Fails when the line coverage of an area is under its floor (reads the Kover report of devDebug)."
	dependsOn("koverXmlReportDevDebug")
	val report = layout.buildDirectory.file("reports/kover/reportDevDebug.xml")
	doLast {
		val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
			setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
		}
		val packages = factory.newDocumentBuilder().parse(report.get().asFile).getElementsByTagName("package")
		// package name -> (covered, total) lines
		val lines = (0 until packages.length).map { packages.item(it) as org.w3c.dom.Element }.associate { pkg ->
			var covered = 0
			var total = 0
			for (k in 0 until pkg.childNodes.length) {
				val node = pkg.childNodes.item(k)
				if (node is org.w3c.dom.Element && node.tagName == "counter" && node.getAttribute("type") == "LINE") {
					covered = node.getAttribute("covered").toInt()
					total = covered + node.getAttribute("missed").toInt()
				}
			}
			pkg.getAttribute("name") to (covered to total)
		}
		val failures = mutableListOf<String>()
		coverageFloors.forEach { (name, prefixes, floor) ->
			val matching = lines.filterKeys { pkg ->
				val path = pkg.removePrefix("com/rizkysiregar/narrativ").removePrefix("/")
				prefixes.any { it.isEmpty() || path == it || path.startsWith("$it/") }
			}.values
			val covered = matching.sumOf { it.first }
			val total = matching.sumOf { it.second }
			val percent = if (total == 0) 0.0 else 100.0 * covered / total
			println("coverage %-40s %5.1f%% (floor %d%%, %d/%d lines)".format(name, percent, floor, covered, total))
			if (percent < floor) failures += "$name is at ${"%.1f".format(percent)}%, the floor is $floor%"
		}
		if (failures.isNotEmpty()) throw GradleException("Coverage is under its floor:\n  " + failures.joinToString("\n  "))
	}
}

val versionMajor = property("VERSION_MAJOR").toString().toInt()
val versionMinor = property("VERSION_MINOR").toString().toInt()
val versionPatch = property("VERSION_PATCH").toString().toInt()
require(versionMinor in 0..99 && versionPatch in 0..99) { "VERSION_MINOR and VERSION_PATCH must be 0..99" }
val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch

android {
	namespace = "com.rizkysiregar.narrativ"
	compileSdk = 36
	flavorDimensions += "env"

	productFlavors {
		create("local") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"http://10.0.2.2:9091/api/v1/\""
			)
			// Web client of the Google Cloud project behind this flavor; the backend verifies the ID token against it.
			buildConfigField("String", "WEB_CLIENT_ID", "\"1036351360011-sgcau0c3kjp1vkea20ijndnj83dpr2eo.apps.googleusercontent.com\"")
		}

		create("dev") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"https://api-dev-narrativ.rizkysiregar.my.id/api/v1/\""
			)
			// Web client of the Google Cloud project behind this flavor; the backend verifies the ID token against it.
			buildConfigField("String", "WEB_CLIENT_ID", "\"1036351360011-sgcau0c3kjp1vkea20ijndnj83dpr2eo.apps.googleusercontent.com\"")
		}

		create("prod") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"https://api-narrativ.rizkysiregar.my.id/api/v1/\""
			)
			// Web client of the Google Cloud project behind this flavor; the backend verifies the ID token against it.
			buildConfigField("String", "WEB_CLIENT_ID", "\"88005068433-dcct46up9nob5upb261rpj648i1pajm7.apps.googleusercontent.com\"")
		}
	}


	defaultConfig {
		applicationId = "com.rizkysiregar.narrativ"
		manifestPlaceholders.putAll(
			mapOf(
				"appAuthRedirectScheme" to "com.rizkysiregar.narrativ"
			)
		)

		minSdk = 26
		targetSdk = 36
		versionCode = appVersionCode
		versionName = appVersionName

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		val storeFile = signingValue("storeFile", "KEYSTORE_FILE")
		if (storeFile != null) {
			create("release") {
				this.storeFile = rootProject.file(storeFile)
				storePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
				keyAlias = signingValue("keyAlias", "KEY_ALIAS")
				keyPassword = signingValue("keyPassword", "KEY_PASSWORD")
			}
		}
	}

	buildTypes {
		debug {
			// The plugin rewrites every OkHttpClient to report to Firebase, which cannot start in a JVM unit test.
			// Debug builds never send telemetry anyway (see TelemetrySettings).
			configure<com.google.firebase.perf.plugin.FirebasePerfExtension> {
				setInstrumentationEnabled(false)
			}
		}
		release {
			signingConfig = signingConfigs.findByName("release")
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlin {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	lint {
		abortOnError = true
		checkReleaseBuilds = true
		// Existing warnings are frozen in the baseline; anything new fails the build.
		baseline = file("lint-baseline.xml")
	}

	testOptions {
		unitTests {
			// Robolectric needs the merged resources; android.util.Log & co return defaults instead of throwing.
			isIncludeAndroidResources = true
			isReturnDefaultValues = true
		}
	}

	packaging {
		// exclude duplicate reference file, usually conflict between dependency
		resources {
			excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
		}
	}

}

dependencies {

	// --- Firebase: push, crash reporting, analytics, performance, remote config, app check ---
	implementation(platform(libs.firebase.bom))
	implementation(libs.firebase.messaging)
	implementation(libs.firebase.analytics)
	implementation(libs.firebase.crashlytics)
	implementation(libs.firebase.perf)
	implementation(libs.firebase.config)
	implementation(libs.firebase.appcheck.playintegrity)
	debugImplementation(libs.firebase.appcheck.debug)

	// --- Core + Compose ---
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.core.splashscreen)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.lifecycle.process)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)

	// per-app language (AppCompatDelegate.setApplicationLocales)
	implementation(libs.androidx.appcompat)
	// material icons
	implementation(libs.androidx.compose.material.icons.core)
	implementation(libs.androidx.compose.material.icons.extended)
	// material2 for: native pull request view
	implementation(libs.androidx.compose.material)

	// --- Compose Navigation + ViewModel ---
	implementation(libs.navigation.compose)
	implementation(libs.lifecycle.viewmodel.compose)

	// --- Coroutines ---
	implementation(libs.coroutines.android)

	// --- Dependency Injection (Hilt) ---
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
	implementation(libs.hilt.navigation.compose)
	implementation(libs.androidx.hilt.work)
	ksp(libs.androidx.hilt.compiler)

	// --- Background work (upload) ---
	implementation(libs.androidx.work.runtime.ktx)

	// --- Networking (Retrofit + OkHttp) ---
	implementation(libs.retrofit)
	implementation(libs.retrofit.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging)
	// In-app network inspector (debug only, no-op in release)
	debugImplementation(libs.chucker)
	releaseImplementation(libs.chucker.no.op)

	// --- DataStore (JWT persistence) ---
	implementation(libs.datastore.preferences)

	// --- Testing ---
	testImplementation(libs.junit)
	testImplementation(libs.kotlinx.coroutines.test)
	testImplementation(libs.turbine)
	testImplementation(libs.truth)
	testImplementation(libs.robolectric)
	testImplementation(libs.androidx.test.core)
	testImplementation(libs.okhttp.mockwebserver)
	testImplementation(libs.okhttp.mockwebserver.junit4)
	testImplementation(libs.androidx.work.testing)
	testImplementation(libs.json.test)
	testImplementation(platform(libs.androidx.compose.bom))
	testImplementation(libs.androidx.ui.test.junit4)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
	androidTestImplementation(platform(libs.androidx.compose.bom))
	androidTestImplementation(libs.androidx.ui.test.junit4)
	debugImplementation(libs.androidx.ui.tooling)
	debugImplementation(libs.androidx.ui.test.manifest)

	// OAuth2
	implementation(libs.play.services.auth)
	implementation(libs.appauth)

	// lottie
	implementation(libs.lottie.compose)

	// image
	implementation(libs.coil.compose)
	implementation(libs.coil.network.okhttp)

	// video playback
	implementation(libs.media3.exoplayer)
	implementation(libs.media3.ui)
	implementation(libs.media3.datasource)

	// video transcoding before upload
	implementation(libs.media3.transformer)
	implementation(libs.androidx.exifinterface)

	// full emoji list in the comment input
	implementation(libs.androidx.emoji2.emojipicker)


	// Credential Manager for Google Sign-In
	implementation(libs.androidx.credentials)
	implementation(libs.androidx.credentials.play.services.auth)
	implementation(libs.googleid)
}