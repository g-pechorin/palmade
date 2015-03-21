# PALMake

CMake front-end for Gradle ... or something near enough you won't notice the difference.

# Goals

* Specify native targets in a pretty way
	* prettier than CMake does anyway
* IDE integration as-good-as CMake already does
* ZeroInstall wrapper-like functionality
	* cheats by scraping a CMake dist and using that
* Spew reams and reams of C++ to combine it with GLFW3 or NDK

# Don't Care

* running `buildCMake` while VisualStudio looking at the project won't work
	* use `listCMake` to regenerate stuff
* the concept of multi-module Gradle builds are ignored

# Usage
	apply plugin: 'palmade'
	buildscript {
		repositories {
			mavenCentral()
			mavenLocal()

			maven {
				name "Peter's Stuff"
				url 'https://dl.dropboxusercontent.com/u/15094498/repo/snap'
			}
		}

		dependencies {
			classpath 'peterlavalle.palmade:buildSrc:0.0.0.0'
		}
	}

# Change Log

## 0.0.0.1

Changes to let me use [GLFW3](http://www.glfw.org/)

* supports "REMOTE" projects where once needs to specify a URL to a `.zip` with the sources
* uses a nicer downloading implementation
* made the tasks prettier

## 0.0.0.0

Initial version

## TODO (aka not-bugs)

My don't hate me, I know it's broken list.
Most of these require deeper knowledge of Gradle than the [User Guide](http://www.gradle.org/docs/current/userguide/userguide), [Basic Book](http://www2.gradleware.com/l/68052/2015-01-13/6dm), or [Advanced Book](http://www2.gradleware.com/ebook) imparted on me.

* test on not-Windows
* get CMake to re-launch the Gradle build
* switch to using `Exec` rather than `ant.exec`
* make prettier `targets {...}` closures
	* I want to use closures that look like `name(Form) { ... }` rather than `name { form = Form ... }`
* de-worm the Target class
	* there is some coupling (noted in comments) which is bad
	* I'm sure that there's a way to share the plugin or some configuration object amongst the tasks and targets
	* REMOTE Targets seem to have a `src/$name/cmake` thing bleeding into the includes

## SOON

* platform/architecture "masks"
	* make things that only exist on "windows" "mac" or whatever
	* make targets that only exist on Android or CMake (see below)
* "pluggable" generators
* generate Android.mk files
