import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

/*
*  Build Types > debug, release
*  Product Flavours > dev, staging, prod
*
* */


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
					"*ViewKt*"
				)
			}
		}
	}
}

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
		}

		create("dev") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"https://api-dev-narrativ.rizkysiregar.my.id/api/v1/\""
			)
		}

		create("prod") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"https://api-narrativ.rizkysiregar.my.id/api/v1/\""
			)
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
		versionCode = 3
		versionName = "1.0.2"

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
	implementation(libs.retrofit.moshi)
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
	implementation("androidx.credentials:credentials:1.6.0")
	implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
	implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")


}