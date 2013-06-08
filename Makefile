.PHONY: debug
debug: build.xml
	ant debug

.PHONY: unsigned-release
unsigned-release:
	ant release

.PHONY: release
release:
	cat .password | ant release

build.xml:
	android update project -p ./

.PHONY: clean
clean: build.xml
	ant clean
	touch build.xml local.properties proguard-project.txt ant.properties .password
	rm build.xml local.properties proguard-project.txt ant.properties .password
