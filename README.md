# HW v1.4.1

> The viewer app of www.hellowork.go.jp.

It can also save the search result.

https://play.google.com/store/apps/details?id=com.kt3k.app.hw

# How to make release build

Put ant.properties at the top of the project with the contents like:

```properties
key.store=Key store file
key.store.password=The password for it
key.alias=Key store alias
key.alias.password=The alias password
```

And then, hit the command

    make release

Then you'll get `bin/hw-release.apk` as a release build.
