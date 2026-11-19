# Build helpers. Run `make help` to list the targets.

GRADLE := ./gradlew
AAB    := app/build/outputs/bundle/prodRelease/app-prod-release.aab
APK    := app/build/outputs/apk/prod/release/app-prod-release.apk

# Release files are copied to dist/ as narrativ-<versionName>-<versionCode>-prod-release.<ext>.
VERSION_MAJOR := $(shell grep '^VERSION_MAJOR=' gradle.properties | cut -d= -f2)
VERSION_MINOR := $(shell grep '^VERSION_MINOR=' gradle.properties | cut -d= -f2)
VERSION_PATCH := $(shell grep '^VERSION_PATCH=' gradle.properties | cut -d= -f2)
VERSION_NAME  := $(VERSION_MAJOR).$(VERSION_MINOR).$(VERSION_PATCH)
VERSION_CODE  := $(shell echo $$(( $(VERSION_MAJOR) * 10000 + $(VERSION_MINOR) * 100 + $(VERSION_PATCH) )))
DIST_NAME     := narrativ-$(VERSION_NAME)-$(VERSION_CODE)-prod-release
DIST_AAB      := dist/$(DIST_NAME).aab
DIST_APK      := dist/$(DIST_NAME).apk

.DEFAULT_GOAL := help
.PHONY: help version clean test build-local build-dev install-local install-dev bundle-prod apk-prod verify-aab release

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-16s %s\n", $$1, $$2}'

version: ## Print versionName and versionCode from gradle.properties
	@echo "versionName $(VERSION_NAME)"
	@echo "versionCode $(VERSION_CODE)"

clean: ## Remove build output
	$(GRADLE) clean

test: ## Run the local debug unit tests
	$(GRADLE) :app:testLocalDebugUnitTest

build-local: ## Build the local debug APK (emulator backend)
	$(GRADLE) :app:assembleLocalDebug

build-dev: ## Build the dev debug APK (dev backend)
	$(GRADLE) :app:assembleDevDebug

install-local: ## Install the local debug build on the connected device
	$(GRADLE) :app:installLocalDebug

install-dev: ## Install the dev debug build on the connected device
	$(GRADLE) :app:installDevDebug

bundle-prod: ## Build the signed prod release AAB for the Play Store
	$(GRADLE) :app:bundleProdRelease
	@mkdir -p dist && cp $(AAB) $(DIST_AAB)
	@echo "AAB: $(DIST_AAB)"

apk-prod: ## Build the signed prod release APK (for installing on a device)
	$(GRADLE) :app:assembleProdRelease
	@mkdir -p dist && cp $(APK) $(DIST_APK)
	@echo "APK: $(DIST_APK)"

verify-aab: ## Print the signing certificate of the prod AAB (expect the upload key)
	@test -f $(DIST_AAB) || { echo "$(DIST_AAB) not found, run 'make bundle-prod' first"; exit 1; }
	@keytool -printcert -jarfile $(DIST_AAB) | grep -E 'Owner|SHA1|SHA256|Valid'

release: clean bundle-prod verify-aab ## Clean build of the prod AAB, then show its certificate
