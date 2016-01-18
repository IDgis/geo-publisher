import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.io.File

/**
 * Concaternates all *.sql files in ${inputDir} and writes
 * a single file named ${destFileName}.sql to ${buildDir}.
 *
 * Statements between '-- <${databaseType}>' and '-- </${databaseType}>'
 * are only included for configured ${databaseType}.
 */
class ConcatSQLTask extends DefaultTask {

	@Input
	String inputDir
	
	@Input
	String buildDir
	
	@Input
	String destFileName
	
	@Input
	String databaseType
	
	@TaskAction
	def concat() {
		def outputDir = new File(project.buildDir, buildDir)
		
		outputDir.mkdirs()
		
		def outputFile = new File(outputDir, destFileName)
		if(outputFile.exists()) {
			outputFile.delete();
		}
		
		def writer = outputFile.newWriter()
		
		def ignorePattern = ~/.*?--.*?<(.*?)>/
		
		project.fileTree(dir: inputDir, includes: ['*.sql'])
			.files
			.toSorted { it.name }
			.each {
				writer.append("\n\n-- ${it.name}\n")
				
				boolean ignore = false
				it.eachLine { line ->
					def ignoreMatcher = ignorePattern.matcher(line.trim())
					if(ignoreMatcher.matches()) {
						def content = ignoreMatcher.group(1)
						if(content.startsWith('/')) {
							ignore = false
						} else {												
							if(content != databaseType) {
								ignore = true
							}
						}
					} else {
						if(ignore) {
							writer.append('-- ')
						}
					}
					
					writer.append(line)
					writer.append('\n')
				}
			}
			
		writer.close()		
	}
}