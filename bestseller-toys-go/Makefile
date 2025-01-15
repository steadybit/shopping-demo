build:
	go build -o bestseller-toys main.go

run:
	go run main.go

docker-build:
	docker build -t bestseller-toys .

docker-run:
	docker run -p 8080:8080 bestseller-toys

## licenses-report: generate a report of all licenses
.PHONY: licenses-report
licenses-report:
ifeq ($(SKIP_LICENSES_REPORT), true)
	@echo "Skipping licenses report"
	rm -rf ./licenses && mkdir -p ./licenses
else
	@echo "Generating licenses report"
	rm -rf ./licenses
	go run github.com/google/go-licenses@v1.6.0 save . --save_path ./licenses --ignore=github.com/xi2/xz
	go run github.com/google/go-licenses@v1.6.0 report . > ./licenses/THIRD-PARTY.csv
	cp LICENSE ./licenses/LICENSE.txt
endif