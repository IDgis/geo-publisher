import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.plugins.ApplicationPlugin

import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

class DockerApplication implements Plugin<Project> {

	void apply(Project project) {
		def moduleName = project.name.substring(project.name.indexOf('-') + 1)
	
		project.pluginManager.apply(ApplicationPlugin)
		project.pluginManager.apply(DockerRemoteConfig)
		
		def unpackTar = project.tasks.create('copyTar', Copy, {
			def distTar = project.tasks.getByName('distTar')
		
			dependsOn distTar
			
			from project.tarTree(distTar.archivePath)
			into "${project.buildDir}/docker"
		})
		
		def createDockerfile = project.tasks.create('createDockerfile', Dockerfile, {
			destFile = project.file("${project.buildDir}/docker/Dockerfile")
			from 'java'
			copyFile "${project.name}-${project.version}", '/opt'
			runCommand "chmod u+x /opt/bin/${project.name}"
			defaultCommand "/opt/bin/${project.name}"
		})
		
		def pullBaseImage = project.tasks.create('pullBaseImage', DockerPullImage, {
			repository 'java'
			tag 'latest'
		})
		
		def buildImage = project.tasks.create('buildImage', DockerBuildImage, {
			dependsOn createDockerfile, pullBaseImage, unpackTar
			
			inputDir = project.file("${project.buildDir}/docker")
			tag = "idgis/geopublisher_${moduleName}:${project.version}"
		})
	}
}