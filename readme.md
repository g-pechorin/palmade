# PALMake

CMake front-end for Gradle ... or something near enough you won't notice the difference.

# Goals

* Specify native targets in a pretty way
	* prettier than CMake does anyway
* IDE integration as-good-as CMake already does
* ZeroInstall wrapper-like functionality
	* cheats by scraping a CMake dist and using that
* Spew reams and reams of C++ to combine it with GLFW3 or NDK and implement SVT in magical shader language

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
