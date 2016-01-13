import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.gradle.api.execution.TaskExecutionListener

class TimingListener extends BuildAdapter implements TaskExecutionListener {
	def beforeTime = [:]
	def afterTime = [:]
	
	@Override
	void beforeExecute(Task task) {
		beforeTime[task] = System.currentTimeMillis()
	}
	
	@Override
	void afterExecute(Task task, TaskState taskState) {
		afterTime[task] = System.currentTimeMillis()
	}
	
	def long startTime = System.currentTimeMillis()
	def percentageFormat = new java.text.DecimalFormat("##.##")
	
	@Override
	void buildFinished(BuildResult result) {
		def currentTime = System.currentTimeMillis() 
		def totalTime = currentTime - startTime
		
		println "Build time: ${totalTime / 1000} secs"
	
		beforeTime.each { task, beforeTime ->
			def taskTime = afterTime[task] - beforeTime
		
			println ":${task.project.name}:${task.name} ${taskTime / 1000} secs (${percentageFormat.format((taskTime / totalTime) * 100)}%)" 
		}
	}
}