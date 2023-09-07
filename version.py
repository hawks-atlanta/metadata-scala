import json

with open("version.json", "rb") as pkg_file:
	pkg = json.load(pkg_file)
	print(f"version={pkg['version']}")