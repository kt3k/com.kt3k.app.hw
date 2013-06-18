.PHONY: debug
debug: build.xml
	ant debug

.PHONY: release
release: build.xml
	ant release

build.xml:
	android update project -p ./ --name 'hw'

.PHONY: clean
clean: build.xml
	ant clean
	touch build.xml local.properties proguard-project.txt ant.properties .password
	rm build.xml local.properties proguard-project.txt ant.properties .password
