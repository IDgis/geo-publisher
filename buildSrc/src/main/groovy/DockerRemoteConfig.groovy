import org.gradle.api.Plugin
import org.gradle.api.Project

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin

class DockerRemoteConfig implements Plugin<Project> {

	void apply(Project project) {
		project.pluginManager.apply(DockerRemoteApiPlugin)
	
		def env = System.env
			project.docker {
				if(env.containsKey('DOCKER_HOST')) {
					url = "$env.DOCKER_HOST"
	
					if(env.containsKey('DOCKER_TLS_VERIFY')) {
						url = url.replace('tcp', 'https')
					} else {
						url = url.replace('tcp', 'http')
					}
	
					if(env.containsKey('DOCKER_CERT_PATH')) {
						certPath = project.file "$env.DOCKER_CERT_PATH"
					}
				} else {
					url = "http://${project.dockerHost}:2375"
				}
		}
	}
}