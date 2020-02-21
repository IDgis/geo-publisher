import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.jvm.tasks.Jar

import org.gradle.play.plugins.PlayPlugin

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

/**
  * Applies standard Gradle Play plugin.
  */
class PublisherPlay implements Plugin<Project> {

	void apply(Project project) {
		project.pluginManager.apply(JavaBasePlugin)
		project.pluginManager.apply(PlayPlugin)
		project.pluginManager.apply(DockerRemoteConfig)
		
		project.model {
			distributions {
				playBinary {
					project.tasks.withType(org.gradle.jvm.tasks.Jar) {
						manifest {
							attributes("Implementation-Title": project.name)
							if(project.version) {
								attributes("Implementation-Version": project.version)
							}
						}
					}
				}
			}
			
			tasks {
				copyTar(Copy) {
					dependsOn createPlayBinaryTarDist
					from project.tarTree("${project.buildDir}/distributions/playBinary.tar")
					into "${project.buildDir}/docker"
				}

				createDockerfile(Dockerfile) {
					dependsOn copyTar
					destFile = project.file('build/docker/Dockerfile')
					from 'azul/zulu-openjdk:8'
					copyFile 'playBinary', '/opt'
					runCommand 'chmod u+x /opt/bin/playBinary'
					exposePort 9000
					defaultCommand "/opt/bin/playBinary"
				}

				buildImage(DockerBuildImage) {
					def moduleName = project.name.substring(project.name.indexOf('-') + 1)
				
					dependsOn project.rootProject.pullJavaImage, createDockerfile
					inputDir = project.file('build/docker')
					tag = "idgis/geopublisher_${moduleName}:${project.version}"
				}
			}
		}
	}
}
