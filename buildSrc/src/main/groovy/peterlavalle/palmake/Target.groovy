package peterlavalle.palmake

import org.gradle.api.Project
import java.util.*

public class Target {

	Form form = Form.STATIC

	final String name

	public Target(String name) {
		this.name = name
	}

	private LinkedHashMap<String, String> sourceFolders = new LinkedHashMap<String, String>()

	public void src(final String path, final String pattern) {
		assert (!sourceFolders.containsKey(path))

		sourceFolders.put(path, pattern)
	}

	public void src(final String path) {
		src(path, '\\w+(/\\w+)*\\.[ch](pp)?')
	}

	public Set<File> sourceFiles(final Project project) {
		def outputs = new HashSet<File>()

		sourceFolders.each { base, pattern ->

			def root = project.file(base)

			def todo = new LinkedList<String>()

			if (null != root.list()) {
				todo.addAll(root.list())
			}

			while (!todo.isEmpty()) {

				def path = todo.removeFirst()
				def file = new File(root, path)

				if (path.matches(pattern)) {
					outputs.add(file)
				}

				def list = file.list()

				if (list != null) {
					for (String sub : list) {
						todo.add(path + "/" + sub)
					}
				}
			}
		}

		return outputs
	}


	private LinkedHashSet<String> libraries = new LinkedHashSet<String>();

	public void lib(final String library) {
		libraries.add(library)
	}

	def getLibraries(final Map<String, Target> targets) {
		def found = new LinkedList<String>()
		def visited = new HashSet<String>()
		def todo = new LinkedList<Target>()

		todo.add(this)

		while (!todo.isEmpty()) {
			def next = todo.removeFirst()

			if (visited.add(next.name)) {

				if (!found.contains(next.name)) {
					found.add(next.name)
				}

				for (String tail : next.libraries) {
					todo.add(targets.get(tail))
				}
			}
		}

		// remove us from the list
		found.removeFirst()

		return found
	}

	final LinkedHashSet<String> includeFolders = new LinkedHashSet<String>();

	public void inc(final String include) {
		includeFolders.add(include)
	}

	/**
	 * Export are #include folders that users of this target should see. This method collects them
	 *
	 * @param project
	 * @return a LinkedList<File> pointing to exported folders
	 */
	def getExports(final Project project) {
		def found = new LinkedList<File>()
		def visited = new HashSet<String>()
		def todo = new LinkedList<Target>()

		todo.add(this);

		for (final String inc : sourceFolders.keySet()) {
			def f = project.file(inc).absoluteFile
			if (!found.contains(f)) {
				found.add(f)
			}
		}

		while (!todo.isEmpty()) {
			def next = todo.removeFirst()

			if (visited.add(next.name)) {
				for (final String inc : next.includeFolders) {
					def f = project.file(inc).absoluteFile

					if (next.form == Form.REMOTE) {
						// NOTE ; this block is tightly-coupled with logic in PALMadePlugin (sorry)

						// Duplicate of the object in the plugin
						// TODO ; work out how to share these configurations
						def cacheDump = new CacheDump(project.file('.cache'), project.file('build/dump'))

						def dir = next.root.split('@')[0] // dir gets us from the root of the dumped-archive to the meta of what we want
						def url = next.root.split('@')[1] // url is the address of the archive that we want

						def absolute = new File(new File(cacheDump.apply(url), dir), inc).absoluteFile

						f = absolute
					}

					if (!found.contains(f)) {
						found.add(f)
					}
				}

				for (final String tail : next.libraries) {
					todo.add(project.targets.asMap.get(tail))
				}
			}
		}

		return found
	}

	private String root

	String getRoot() {
		return root
	}

	public void setRoot(String root) {

		assert libraries.isEmpty()
		assert 1 == sourceFolders.size()

		// death or glory
		libraries = null
		sourceFolders = null

		assert null == this.root

		this.root = root
	}
}