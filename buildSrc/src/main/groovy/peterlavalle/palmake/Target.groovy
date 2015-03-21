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

						def dir = next.root.split('@')[0]
						def url = next.root.split('@')[1]
						def ext = url.substring(url.lastIndexOf('.') + 1)
						def zip = url.substring(0, url.lastIndexOf('.')).replaceAll("\\W", "_") + ".$ext"

						f = project.file("build/dump/$zip/$dir/$inc").absoluteFile
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