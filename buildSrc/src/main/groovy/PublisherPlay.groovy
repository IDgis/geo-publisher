import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar

import org.gradle.play.plugins.PlayPlugin

import com.github.houbie.gradle.lesscss.LesscTask

class PublisherPlay implements Plugin<Project> {

	void apply(Project project) {
		project.pluginManager.apply(PlayPlugin)
		
		project.model {
			components {
				play {
				
					binaries.all { binary ->
						def lessDestinationDir = "${project.buildDir}/less/"
						def extractLessTask = tasks.taskName('extract', 'less')

						binary.assets.addAssetDir project.file(lessDestinationDir)

						tasks.create(extractLessTask, Copy) { task ->
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
							sourceDir 'app/assets', lessDestinationDir
							include '**/*.less'
							exclude '**/_*.less'
							exclude 'lib/**'
							destinationDir = project.file("${project.buildDir}/${binary.name}/lessAssets")

							binary.assets.addAssetDir destinationDir
							binary.assets.builtBy task
							dependsOn extractLessTask
						}
					
						def fixTask = project.tasks.create(tasks.taskName('fix', 'twirlTemplates'))
											
						fixTask << {
							binary.generatedScala.each { generated ->
								if(generated.key.name == 'twirlTemplates') {
									generated.value.source.visit { item ->
										if(!item.isDirectory()) {
											def sourceLines = item.file.readLines();

											sourceLines.remove(6);

											['import play.api.templates.PlayMagic._',
											'import models._',
											'import controllers._',
											'import java.lang._',
											'import java.util._',
											'import scala.collection.JavaConversions._',
											'import scala.collection.JavaConverters._',
											'import play.api.i18n._',
											'import play.core.j.PlayMagicForJava._',
											'import play.mvc._',
											'import play.data._',
											'import play.api.data.Field',
											'import play.mvc.Http.Context.Implicit._',
											'import views.html._'].eachWithIndex { line, lineNo ->
												sourceLines.add(6 + lineNo, line)
											}

											item.file.write(sourceLines.join('\n'))
										}
									}
								}
							}
						}

						tasks.whenObjectAdded { task ->
							if(task.name == tasks.taskName('compile', 'twirlTemplates')) {
								fixTask.dependsOn(task)
							}

							if(task.name == tasks.taskName('compile', 'scala')) {
								task.dependsOn(fixTask)
							}
						}
					}
				}
			}
		}
	}
}