import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
}

android {
	namespace = "com.rizkysiregar.feedzmore"
	compileSdk = 35
	flavorDimensions += "env"

	productFlavors {

		create("local-emulator") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"http://10.0.2.2:9091/api/v1/\""
			)
		}

		create("local-real-device") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"http://192.168.1.16:9091/api/v1/\""
			)
		}

		create("dev") {
			dimension = "env"
			buildConfigField(
				"String",
				"BASE_URL",
				"\"https://feedzmore-dev.rizkysiregar.my.id/api/v1/\""
			)
		}
	}


	defaultConfig {
		applicationId = "com.rizkysiregar.feedzmore"
		manifestPlaceholders.putAll(
			mapOf(
				"appAuthRedirectScheme" to "com.rizkysiregar.feedzmore"
			)
		)

		minSdk = 26
		targetSdk = 35
		versionCode = 2
		versionName = "1.0.1"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
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

	packaging {
		// exclude duplicate reference file, usually conflict between dependency
		resources {
			excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
		}
	}

}

dependencies {

	// --- Core + Compose ---
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.androidx.ui)
	implementation(libs.androidx.ui.graphics)
	implementation(libs.androidx.ui.tooling.preview)
	implementation(libs.androidx.material3)

	// --- Compose Navigation + ViewModel ---
	implementation(libs.navigation.compose)
	implementation(libs.lifecycle.viewmodel.compose)

	// --- Coroutines ---
	implementation(libs.coroutines.android)

	// --- Dependency Injection (Hilt) ---
	implementation(libs.hilt.android)
	ksp(libs.hilt.compiler)
	implementation(libs.hilt.navigation.compose)

	// --- Networking (Retrofit + OkHttp) ---
	implementation(libs.retrofit)
	implementation(libs.retrofit.moshi)
	implementation(libs.retrofit.gson)
	implementation(libs.okhttp)
	implementation(libs.okhttp.logging)

	// --- DataStore (JWT persistence) ---
	implementation(libs.datastore.preferences)

	// --- Testing ---
	testImplementation(libs.junit)
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

	// material2
	implementation(libs.androidx.compose.material)

	// image
	implementation(libs.coil.compose)
	implementation(libs.coil.network.okhttp)


}