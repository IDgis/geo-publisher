import org.gradle.api.Plugin
import org.gradle.api.Project

import org.ajoberstar.grgit.Grgit

class GitVersion implements Plugin<Project> {

	void apply(Project project) {
		def repo = Grgit.open(project.file('.'))
		def tag = repo.describe()
		def prefix = "${project.name}-"
		
		if(tag.startsWith(prefix)) {
			project.version = tag.substring(prefix.length())
			if(project.version.contains('-g')) {
				project.version += '-SNAPSHOT'
			}
		}
		
		def gitVersion = project.task('gitVersion') {
			group = 'Help'
			description = 'Displays version as derived from git describe'
		}
		
		gitVersion << {
			println "gitVersion: ${if(project.version) { project.version } else { 'undefined' } }"
		}
	}
}