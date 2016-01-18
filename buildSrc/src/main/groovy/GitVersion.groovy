import org.gradle.api.Plugin
import org.gradle.api.Project

import org.ajoberstar.grgit.Grgit

/**
 * Derives a version from git describe. Expects tags
 * in ${project.name}-${project.version} format.
 */
class GitVersion implements Plugin<Project> {

	void apply(Project project) {
		def repo = Grgit.open(project.file('.'))
		def describe = repo.describe()
		def prefix = "${project.name}-"
		
		if(describe.startsWith(prefix)) {
			project.version = describe.substring(prefix.length())
			if(project.version.contains('-g')) { // HEAD contains additional commits
				project.version += '-SNAPSHOT'
			}
		}
		
		// add a help task to display the resulting version
		def gitVersion = project.task('gitVersion') {
			group = 'Help'
			description = 'Displays version as derived from git describe'
		}
		
		gitVersion << {
			println "git describe: ${describe} version: ${project.version}"
		}
	}
}