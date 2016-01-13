import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.artifacts.Configuration

import com.mysema.query.sql.codegen.MetaDataExporter

import groovy.sql.Sql

class MetaDataExporterTask extends DefaultTask {

	@Input
	String url
	
	@Input
	String user
	
	@Input
	String password
	
	@Input
	Configuration configuration
	
	@Input
	String driverClassName

	@Input
	String packageName

	@Input	
	File targetDir
	
	@TaskAction
	def export() {
		def loader = GroovyObject.class.classLoader
		configuration.each { file ->
			loader.addURL(file.toURL())
		}
	
		def sql = Sql.newInstance(url, user, password, driverClassName)
	
		MetaDataExporter exporter = new MetaDataExporter()
		exporter.setPackageName packageName
		exporter.setTargetFolder targetDir
		exporter.export sql.getConnection().getMetaData()
		
		sql.close()
	}
}