package peterlavalle.palmake;

import org.gradle.api.Project;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public enum Form {
	PROGRAM,
	MODULE,
	STATIC,



	REMOTE, /*
			url(http, path-to-cmake)
			
			inc(la, la, la)
		*/

	EXTERN; // just a declaration - no source or whatever. really just says "shutup I know what I'm doing"
}
