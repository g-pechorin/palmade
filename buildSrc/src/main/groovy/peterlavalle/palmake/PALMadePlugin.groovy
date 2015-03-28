package peterlavalle.palmake

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import peterlavalle.palmade.*


class PALMadePlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		project.extensions.cacheDump = new CacheDump(project.file('.cache'), project.file('build/dump'))

		project.extensions.targets = project.container(PALTarget, { name ->
			return new PALTarget(name, project)
		})

		project.task('listCMake') {

			group = 'Build Setup'
			description = 'Generates CMakeLists files for defined targets'

			ext.listDir = project.file("$project.buildDir/cmake-lists")

			doLast {

				assert (listDir.exists() || listDir.mkdirs())
				def master = new FileWriter(new File((File) listDir, 'CMakeLists.txt'))

				master.append('cmake_minimum_required (VERSION 2.8)\n')
				master.append('\n')
				master.append('if(WIN32)\n')
				master.append('\tadd_definitions(-D_CRT_SECURE_NO_WARNINGS)\n')
				master.append('else(WIN32)\n')
				master.append('\tset(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -std=gnu++11 -static-libstdc++")\n')
				master.append('\tset(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++")\n')
				master.append('endif(WIN32)\n')
				master.append('\n')
				master.append('\n')
				master.append("project($project.name)\n")
				master.append('\n')

				project.targets.each {
					target ->
						if (target.form == Form.EXTERN$.MODULE$) {
							master.append("\t# $target.name is EXTERN\n")
						} else if (target.isRemotelyProvided('CMakeLists.txt')) {
							def path = target.locate('CMakeLists.txt').parentFile.absolutePath.replace('\\', '/')
							def name = target.name
							master.append("\tadd_subdirectory($path $name)\n")
						} else {
							def listFile = new File(new File((File) listDir, target.name), 'CMakeLists.txt')
							assert (listFile.getParentFile().exists() || listFile.getParentFile().mkdirs())

							CMakeList.apply(new FileWriter(listFile), target).close()

							master.append("\tadd_subdirectory($target.name)\n")
						}
				}
				master.close()
			}
		}

		project.task('scrapeCMake') {

			group = 'Build Setup'
			description = 'Downloads a CMake archive for your platform'

			doLast {
				if (Os.isFamily(Os.FAMILY_WINDOWS)) {
					ext.exe = new File(new File(project.extensions.cacheDump.apply('http://www.cmake.org/files/v3.2/cmake-3.2.1-win32-x86.zip'), 'bin'), 'cmake.exe')
				} else {
					println('FIXME ; not-Windows stuff needs to be updated')
					if (Os.isFamily(Os.FAMILY_MAC)) {
						ext.url = 'http://www.cmake.org/files/v3.2/cmake-3.2.1-Darwin-x86_64.tar.gz'
					} else {
						assert (Os.isFamily(Os.FAMILY_UNIX))
						if (Os.isArch('x86')) {
							ext.url = 'http://www.cmake.org/files/v3.2/cmake-3.2.1-Linux-i386.tar.gz'
						} else {
							ext.url = 'http://www.cmake.org/files/v3.2/cmake-3.2.1-Linux-x86_64.tar.gz'
						}
					}
				}
			}
		}

		project.task('cacheCMake') {
			dependsOn(project.scrapeCMake)
			dependsOn(project.listCMake)

			group = 'Build Setup'
			description = 'Uses the generated CMakeLists to generate default platform build files'

			ext.cacheDir = project.file('build/cmake-cache')

			// since CMake isn't an "in source build" we can use these
			// ... if you use an "in source build" this might burst into flames
			inputs.dir project.listCMake.listDir
			outputs.dir cacheDir

			doLast {
				assert (cacheDir.exists() || cacheDir.mkdirs())

				ant.exec(executable: project.scrapeCMake.exe, dir: cacheDir) {
					arg(value: project.listCMake.listDir.absolutePath)
				}
			}
		}

		if (false) // TODO ; make this work so that I get proper output, errors, and result codes
			project.tasks.create(name: 'targetCMake', type: org.gradle.api.tasks.Exec) {

				workingDir project.cacheCMake.cacheDir

				dependsOn(project.scrapeCMake)
				dependsOn(project.cacheCMake)

				ext.config = 'Release'
				commandLine project.tasks.scrapeCMake.exe, '--build', project.tasks.cacheCMake.cacheDir, '--config', config
			}

		project.task('buildCMake') {
			dependsOn(project.scrapeCMake)
			dependsOn(project.cacheCMake)

			group = 'Build Setup'
			description = 'Uses CMake\'s --build command to build a --config'

			ext.config = 'Release'

			doLast {
				assert (config.matches('Release|Debug'))

				// println(project.scrapeCMake.exe)
				// println(project.cacheCMake.cacheDir)

				ant.exec(executable: project.scrapeCMake.exe, dir: project.cacheCMake.cacheDir) {
					arg(value: '--build')
					arg(value: project.cacheCMake.cacheDir)
					arg(value: '--config')
					arg(value: config)
				}
			}
		}
	}
}