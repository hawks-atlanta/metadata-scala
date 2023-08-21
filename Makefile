WORKING_DIR=$(shell pwd)
BASE_SOURCE_DIR="src/main/scala"
BASE_TEST_DIR="src/test/scala"

# Create a new package with the following structure:
# package_name
# 	- domain
# 	- application
# 	- infrastructure
create:
	@read -p "Enter the name of the package: " package_name; \
	cd $(BASE_SOURCE_DIR); \
	mkdir $$package_name; \
	mkdir $$package_name/domain $$package_name/application $$package_name/infrastructure; \
  	cd $(WORKING_DIR); \
	cd $(BASE_TEST_DIR); \
	mkdir $$package_name;

# Remove a package
remove:
	@read -p "Enter the name of the package: " package_name; \
	cd $(BASE_SOURCE_DIR); \
	rm -rf $$package_name; \
	cd $(WORKING_DIR); \
	cd $(BASE_TEST_DIR); \
	rm -rf $$package_name;

coverage:
	export DATABASE_HOST=localhost; \
  	export DATABASE_PORT=5432; \
  	export DATABASE_NAME=metadata; \
  	export DATABASE_USER=postgres; \
  	export DATABASE_PASSWORD=postgres; \
	sbt -DDATABASE_HOST=$$DATABASE_HOST -DDATABASE_PORT=$$DATABASE_PORT -DDATABASE_NAME=$$DATABASE_NAME -DDATABASE_USER=$$DATABASE_USER -DDATABASE_PASSWORD=$$DATABASE_PASSWORD clean coverage test coverageReport