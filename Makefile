# ==================================================================================== #
# HELPERS
# ==================================================================================== #

## help: print this help message
.PHONY: help
help:
	@echo 'Usage:'
	@sed -n 's/^##//p' ${MAKEFILE_LIST} | column -t -s ':' |  sed -e 's/^/ /'


## charttesting: Run Helm chart unit tests
.PHONY: charttesting
charttesting:
	@set -e; \
	for dir in charts/shopping-demo; do \
		echo "Unit Testing $$dir"; \
		helm unittest $$dir; \
	done

## chartlint: Lint charts
.PHONY: chartlint
chartlint:
	ct lint --config chartTesting.yaml

## chart-bump-version: Bump the patch version and optionally set the appVersion
.PHONY: chart-bump-version
chart-bump-version:
	@set -e; \
	for dir in charts/steadybit-extension-*; do \
		if [ ! -z "$(APP_VERSION)" ]; then \
					yq -i ".appVersion = strenv(APP_VERSION)" $$dir/Chart.yaml; \
		fi; \
		CHART_VERSION=$$(semver -i patch $$(yq '.version' $$dir/Chart.yaml)) \
		yq -i ".version = strenv(CHART_VERSION)" $$dir/Chart.yaml; \
		grep -e "^version:" -e "^appVersion:" $$dir/Chart.yaml; \
	done
