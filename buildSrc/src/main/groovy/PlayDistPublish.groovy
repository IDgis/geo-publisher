import org.gradle.api.Task

import org.gradle.model.RuleSource
import org.gradle.model.Mutate
import org.gradle.model.ModelMap

import org.gradle.play.distribution.PlayDistributionContainer

import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension

/**
  * Adds ${project.name} and ${project.version} to jar manifest 
  * and configures distributions as publications.
  */
class PlayDistPublish extends RuleSource {	
	@Mutate void addAttributes(ModelMap<Task> tasks, PlayDistributionContainer dists, PublishingExtension publishing) {
		dists.names.each {
			def jarTask = tasks.get("create${it.capitalize()}DistributionJar")
			
			jarTask.configure {
				manifest {
					attributes("Implementation-Title": project.name)
					if(project.version) {
						attributes("Implementation-Version": project.version)
					}
				}
			}
			
			def zipTask = tasks.get("create${it.capitalize()}Dist")
			
			publishing.publications {
				mavenJava(MavenPublication) {
					groupId 'nl.idgis.publisher'
					artifact(zipTask) {
						classifier 'app'
					}
				}
			}
		}
	}
}