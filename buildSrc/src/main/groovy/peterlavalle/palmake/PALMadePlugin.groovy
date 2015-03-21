package peterlavalle.palmake

import org.apache.tools.ant.taskdefs.condition.Os

import org.gradle.api.GradleScriptException
import org.gradle.api.Plugin
import org.gradle.api.Project


class PALMadePlugin implements Plugin<Project> {
	void apply(Project project) {
		def targets = project.container(Target)
		project.extensions.targets = targets
		targets.all {
			src "src/$name/cmake"
			inc "src/$name/cmake"
		}

		def cacheDump = new CacheDump(project.file('.cache'), project.file('build/dump'))

		project.task('listCMake') {

			ext.listDir = project.file('build/cmake-lists')

			description 'Generates CMakeLists.txt files for defined targets'

			doLast {
				assert (listDir.exists() || listDir.mkdirs())
				def master = new FileWriter(new File(listDir, 'CMakeLists.txt'))

				master.append('cmake_minimum_required (VERSION 2.8)\n')
				master.append('\n')
				master.append('add_definitions(-D_CRT_SECURE_NO_WARNINGS)\n')
				master.append('set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -std=gnu++11 -static-libstdc++")\n')
				master.append('\n')
				master.append('set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libstdc++")\n')
				master.append("project($project.name)\n")
				master.append('\n')
				targets.each { target ->

					if (target.form != Form.REMOTE) {
						def sourceFiles = target.sourceFiles(project)

						// ...
						def listFile = new File(listDir, "$target.name/CMakeLists.txt")
						assert (listFile.getParentFile().exists() || listFile.getParentFile().mkdirs())
						CMakeList.apply(new FileWriter(listFile),
								target.name, target.form,
								(Set<File>) sourceFiles,
								(target.form == Form.PROGRAM || target.form == Form.MODULE) ? (List<String>) target.getLibraries(project.targets.asMap) : new java.util.LinkedList<String>(),
								(List<String>) (target.getExports(project))
						).close()
						master.append("\tadd_subdirectory($target.name)\n")

					} else {

						def dir = target.root.split('@')[0]
						def url = target.root.split('@')[1]

						def absolute = new File(cacheDump.apply(url), dir).absolutePath.replace('\\', '/')

						master.append("\tadd_subdirectory($absolute $target.name)\n")

						/*
						ant.get(
								src: url,
								dest: project.file(".cache/$zip"),
								skipexisting: true
						)

						ant.unzip(
								src: project.file(".cache/$zip"),
								dest: project.file("build/dump/$zip")
						)
						*/
					}

				}
				master.close()
			}
		}

		project.task('scrapeCMake') {

			doLast {
				if (Os.isFamily(Os.FAMILY_WINDOWS)) {
					ext.exe = new File(new File(cacheDump.apply('http://www.cmake.org/files/v3.2/cmake-3.2.1-win32-x86.zip'), 'bin'), 'cmake.exe')
				} else if (Os.isFamily(Os.FAMILY_MAC)) {
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

		project.task('cacheCMake') {
			dependsOn(project.scrapeCMake)
			dependsOn(project.listCMake)

			// since it's not an "in source build" we can do it incrementally
			ext.cacheDir = project.file('build/cmake-cache')


			inputs.dir project.listCMake.listDir
			outputs.dir cacheDir

			doLast {
				assert (cacheDir.exists() || cacheDir.mkdirs())

				ant.exec(executable: project.scrapeCMake.exe, dir: cacheDir) {
					arg(value: project.listCMake.listDir.absolutePath)
				}
			}
		}

		if (false) // TODO ; make this work
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

			ext.config = 'Release'

			doLast {
				// println(project.scrapeCMake.exe)
				// println(project.cacheCMake.cacheDir)

				assert (config.matches('Release|Debug'))
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