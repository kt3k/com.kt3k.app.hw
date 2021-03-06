.PHONY: debug release clean device-test device-install device-app-launch device-logcat avd

NPM_BIN=`npm bin`

debug: build.xml
	ant debug

release: build.xml assets
	ant release

build.xml:
	android update project -p ./ --name 'hw'

assets:
	$(NPM_BIN)/bulbo build

clean: build.xml
	ant clean
	touch build.xml local.properties proguard-project.txt ant.properties .password
	rm build.xml local.properties proguard-project.txt ant.properties .password
	rm -rf assets

device-test: device-install device-app-launch device-logcat
	@echo

device-install:
	adb install -r bin/hw-release.apk

device-app-launch:
	adb shell am start -n com.kt3k.app.hw/com.kt3k.app.hw.BaseActivity

device-logcat:
	adb logcat -s *:E chromium

avd:
	android avd
