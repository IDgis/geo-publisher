import org.gradle.api.Task

import org.gradle.model.RuleSource
import org.gradle.model.Mutate
import org.gradle.model.ModelMap

import org.gradle.play.internal.PlayApplicationBinarySpecInternal

class CustomPlayHttpPort extends RuleSource {

	@Mutate void configureRunTask(ModelMap<Task> tasks, ModelMap<PlayApplicationBinarySpecInternal> playBinaries) {
		tasks.get('runPlayBinary').configure {
			httpPort = project.playHttpPort
		}
	}
}