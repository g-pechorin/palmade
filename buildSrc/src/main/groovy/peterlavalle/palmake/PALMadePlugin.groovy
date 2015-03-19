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

					def sourceFiles = target.sourceFiles(project)

					// ...
					def listFile = new File(listDir, "$target.name/CMakeLists.txt")
					assert (listFile.getParentFile().exists() || listFile.getParentFile().mkdirs())
					CMakeList.apply(new FileWriter(listFile),
							target.name, target.form,
							(java.util.Set<File>) sourceFiles,
							(target.form == Form.PROGRAM || target.form == Form.MODULE) ? (java.util.List<String>) target.getLibraries(project.targets.asMap) : new java.util.LinkedList<String>(),
							(java.util.List<String>) (target.getExports(project))
					).close()
					master.append("\tadd_subdirectory($target.name)\n")
				}
				master.close()
			}
		}


		project.task('scrapeCMake') {

			if (Os.isFamily(Os.FAMILY_WINDOWS)) {
				ext.url = 'http://www.cmake.org/files/v3.2/cmake-3.2.1-win32-x86.zip'
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

			ext.cacheDir = project.file('.cache')
			ext.dumpDir = project.file('build/dump')

			doLast {
				assert (cacheDir.exists() || cacheDir.mkdirs())
				assert (dumpDir.exists() || dumpDir.mkdirs())

				def archive = url.substring(url.lastIndexOf('/') + 1)
				def cache = new File(cacheDir, archive)
				def dump = dumpDir

				ant.get(
						src: url,
						dest: cache,
						skipexisting: true
				)

				if (Os.isFamily(Os.FAMILY_WINDOWS)) {
					ant.unzip(
							src: cache,
							dest: dump
					)
					ext.exe = new File(dumpDir, archive.substring(0, archive.lastIndexOf('.')) + '/bin/cmake.exe')
				} else {
					ant.untar(
							src: cache,
							dest: dump,
							compression: 'gzip'
					)
					ext.exe = null
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

		project.task('buildCMake') {
			dependsOn(project.scrapeCMake)
			dependsOn(project.cacheCMake)

			ext.config = 'Release'

			doLast {
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