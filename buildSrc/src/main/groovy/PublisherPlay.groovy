import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.jvm.tasks.Jar

import org.gradle.play.plugins.PlayPlugin

import com.github.houbie.gradle.lesscss.LesscTask

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

/**
  * Applies standard Gradle Play plugin and adds the following additional tasks:
  *
  * - extract less: extracts all less source files from webjars.
  * - compile less: compiles all less source files.
  */
class PublisherPlay implements Plugin<Project> {

	void apply(Project project) {
		project.pluginManager.apply(JavaBasePlugin)
		project.pluginManager.apply(PlayPlugin)
		project.pluginManager.apply(DockerRemoteConfig)
		
		project.model {
			components {
				play {
				
					binaries.all { binary ->
						def lessDestinationDir = "${project.buildDir}/less/"
						def extractLessTask = tasks.taskName('extract', 'less')

						binary.assets.addAssetDir project.file(lessDestinationDir)

						tasks.create(extractLessTask, Copy) { task ->
							description = 'Extracts all less source files from webjars'
							from {
								project.configurations.play.collect { 
									project.zipTree(it).matching { include 'META-INF/resources/webjars/**' }
								}
							}
							into lessDestinationDir + "lib/"
							eachFile { details ->
								def shortPath = (details.path - "META-INF/resources/webjars/")
								def parts = shortPath.split '/'
								def result = new StringBuilder ()
								for(int i = 0; i < parts.length; ++ i) {
									if(i == 1) {
										continue;
									}
									if(result.length () > 0) {
										result.append "/"
									}
									result.append parts[i]
								}
								def targetPath = result.toString ()
								details.path = targetPath
							}

							binary.assets.builtBy task
						}

						project.tasks.create(tasks.taskName('compile', 'less'), LesscTask) { task ->
							description = 'Compiles all less source files'
							sourceDir 'app/assets', lessDestinationDir
							include '**/*.less'
							exclude '**/_*.less'
							exclude 'lib/**'
							destinationDir = project.file("${project.buildDir}/${binary.name}/lessAssets")

							binary.assets.addAssetDir destinationDir
							binary.assets.builtBy task
							dependsOn extractLessTask
						}
					}
				}
			}
			
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
					from 'azul/zulu-openjdk'
					copyFile 'playBinary', '/opt'
					runCommand 'chmod u+x /opt/bin/playBinary'
					exposePort 9000
					defaultCommand "/opt/bin/playBinary"
				}

				buildImage(DockerBuildImage) {
					def moduleName = project.name.substring(project.name.indexOf('-') + 1)
				
					dependsOn project.rootProject.pullImage, createDockerfile
					inputDir = project.file('build/docker')
					tag = "idgis/geopublisher_${moduleName}:${project.version}"
				}
			}
		}
	}
}