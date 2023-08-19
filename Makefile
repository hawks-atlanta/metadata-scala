BASEDIR="src/main/scala"

# Create a new package with the following structure:
# package_name
# 	- domain
# 	- application
# 	- infrastructure
create:
	@read -p "Enter the name of the package: " package_name; \
	cd $(BASEDIR); \
	mkdir $$package_name; \
	mkdir $$package_name/domain $$package_name/application $$package_name/infrastructure;

# Remove a package
remove:
	@read -p "Enter the name of the package: " package_name; \
	cd $(BASEDIR); \
	rm -rf $$package_name;