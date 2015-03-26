package peterlavalle.palmade

import java.io.File

import org.gradle.api.internal.FactoryNamedDomainObjectContainer
import peterlavalle.palmade.Form.REMOTE

import scala.collection.JavaConversions._
import java.util
import org.gradle.api.Project

case class PALTarget(name: String) {
	var form: Form.TFormat = Form.STATIC

	var project: Project = _
	var cacheDump: CacheDump = _

	def locate(path: String): File =
		form match {
			case Form.REMOTE(location) =>
				val Array(dir, url) = location.split('@')

				require(null != name)
				require(null != dir)
				require(null != url)
				require(null != cacheDump)

				new File(new File(cacheDump(url), dir), path)
			case _ =>
				project.file(path)
		}

	/**
	 * Does the remote project provide this file?
	 *
	 * Used to check for CMakeLists.txt in downloaded files
	 *
	 * @param name of the file
	 * @return is this a REMOTE and does the file exist
	 */
	def isRemotelyProvided(name: String) =
		form match {
			case Form.REMOTE(location: String) =>
				val Array(dir, url) = location.split('@')
				locate(name).exists()
			case _ => false
		}


	/**
	 * Finds another target by name
	 * @param peer
	 * @return
	 */
	private def peerTarget(peer: String): PALTarget =
		project.getExtensions.findByName("targets")
			.asInstanceOf[FactoryNamedDomainObjectContainer[PALTarget]].findByName(peer)

	//// ====================
	// Source handling

	private val sourceFolders = new util.LinkedList[(String, String)]()

	def src(path: String, pattern: String): Unit = sourceFolders.add(path -> pattern)

	def src(path: String): Unit = src(path, "\\w+(/\\w+)*\\.[ch](pp)?")

	def srcPaths = sourceFolders.flatMap {
		case (path: String, pattern: String) =>
			val root =
				form match {
					case Form.MODULE | Form.PROGRAM | Form.STATIC =>
						project.file(path)

					case Form.REMOTE(location: String) =>
						val Array(dir, url) = location.split('@')

						new File(cacheDump(url), dir)

					case Form.EXTERN =>
						sys.error(toString + " is Extern and has no sources")
				}

			def recu(todo: List[String], done: List[String]): List[String] =
				todo match {
					case Nil => done.reverse
					case head :: tail =>
						val file = new File(root, head)
						recu(
							file.list() match {
								case null => tail
								case list => tail ++ (list.map(head + "/" + _))
							},
							if (head.matches(pattern))
								file.getAbsolutePath.replace('\\', '/') :: done
							else
								done
						)
				}

			root.list() match {
				case null => List()
				case list => recu(list.toList, List())
			}
	}

	//// --------------------


	//// ====================
	// Library / dependency handling

	private val libraries = new util.LinkedHashSet[String]()

	def lib(library: String) {
		require(!libraries.contains(library))
		libraries += library
	}

	def libNames = {
		def recu(todo: List[String], done: List[String]): List[String] = {
			todo match {
				case Nil =>
					done.reverse

				case peer :: tail =>
					recu(peerTarget(peer).libraries.toList.filterNot(done.contains) ++ tail, peer :: done)
			}
		}

		recu(libraries.toList, List())
	}

	//// --------------------


	//// ====================
	// Export and Include handling

	/**
	 * These are explicitly added to us and anyone who depends on us
	 */
	private val exported = new util.LinkedHashSet[String]()

	def exp(path: String) {
		require(!exported.contains(path))
		exported += path
	}

	def expPaths: List[String] = {
		require(null != cacheDump)

		val mine: List[String] =
			exported.toList.map(locate).map(_.getAbsolutePath.replace('\\', '/'))

		libNames
			.map(peerTarget)
			.map {
			case peer =>
				peer.cacheDump = cacheDump
				peer.expPaths
		}
			.foldLeft(mine)(_ ++ _)
	}

	/**
	 * These are what we need to include (... so our sources and our peer's exports)
	 */
	def incPaths = {
		require(null != cacheDump)
		libNames.map(peerTarget).map(_.expPaths).foldLeft(sourceFolders.map(_._1).map(project.file).map(_.getAbsolutePath.replace('\\', '/'))) {
			case (found, next) =>
				found ++ next.filterNot(found.contains)
		}
	}

	//// --------------------


	def Module {
		require(form == Form.STATIC)
		form = Form.MODULE
	}

	def Static {
		require(form == Form.STATIC)
		form = Form.STATIC
	}

	def Program {
		require(form == Form.STATIC)
		form = Form.PROGRAM
	}

	def Extern {
		require(form == Form.STATIC)
		form = Form.EXTERN
	}

	def Remote(url: String) {
		Remote(url, null)
	}

	def Remote(url: String, md5: String) {
		require(form == Form.STATIC)
		form = REMOTE(url)(md5)
	}


}
