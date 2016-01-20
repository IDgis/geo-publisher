package router.dav

import play.api.routing._
import play.api.mvc._

import views.xml.dav

import model.dav._

import java.util.stream.{Stream => JStream}
import java.util.stream.Stream.{concat, empty => emptyJStream}
import java.util.{Optional => JOptional}
import java.util.Optional.{empty => emptyJOptional}
import java.util.{List => JList}
import java.util.{Arrays => JArrays}
import java.util.Collections.{emptyList => emptyJList}

import scala.collection.JavaConversions._

/** Simple read-only WebDAV implementation.
  *  
  * This router provides a minimal WebDAV implementation for creating a
  * single read-only WebDAV folder.
  */
abstract class SimpleWebDAV(val prefix: String, val directories: JList[SimpleWebDAV]) extends Router {
  require(prefix.endsWith("/"), "WebDAV url should have a trailing '/'")
  
  // prepare documentation lines,
  // these lines are shown when no action is found
  val methodPrefix = getClass().getCanonicalName() + ".";
  
  def documentation: Seq[(String, String, String)] = {
    val folders =
      for(
        directory <- directories;
        item <- directory.documentation)
          yield item
          
    List() ++ folders ++ List(
     ("PROPFIND", prefix, methodPrefix + "descriptions"),
     ("PROPFIND", prefix + "*", methodPrefix + "properties"),
     ("GET", prefix + "*", methodPrefix + "resource"))
  }
  
  // no argument constructor is required to include
  // this router in static routes
  def this() = this("/", emptyJList())
  
  def this(prefix: String) = this(prefix, emptyJList())
  
  /** Called to fetch a list of resources in the WebDAV folder. */  
  def descriptions(): JStream[ResourceDescription] = emptyJStream()
  
  /** Called to fetch the properties of a resource. */
  def properties(name: String): JOptional[ResourceProperties] = emptyJOptional()
  
  /** Called to fetch a resource. */
  def resource(name: String): JOptional[Resource] = emptyJOptional()
  
  private def resourceName(path :String) = path.substring(prefix.length())
  
  // JStream.of with single parameter is ambiguous (i.e. unusable) in Scala
  private def of[T](t: T*): JStream[T] = t.stream()
  
  override def routes = {
    if(directories.isEmpty) {
      folderRoutes
    }
    else {
      // merge routes
      val elseRoutes =
        for(directory <- directories)
          yield directory.routes
      
      val elseRoute = elseRoutes reduceLeft (_ orElse _)
      elseRoute orElse folderRoutes
    }
  }
  
  def folderRoutes: Router.Routes = {
    // fetch resource
    case rh: RequestHeader if rh.method == "GET"
        && rh.path.length() != prefix.length()
        && rh.path.startsWith(prefix) => Action {
          
        val r = resource(resourceName(rh.path))
        if(r.isPresent()) {
          Results.Ok(r.get.content) withHeaders(("Content-Type", r.get.contentType))
        } else {
          Results.NotFound("404 Not Found: " + rh.path)
        }
    }
    
    case rh: RequestHeader if rh.method == "PROPFIND" 
        && rh.path.startsWith(prefix) => Action {
      
      Results.Status(207) {
        if(rh.path == prefix) {
          // list all resources in folder
          val root = new DefaultResourceDescription("", new DefaultResourceProperties(true)) 
          
          val folders: Seq[ResourceDescription] =
          for(directory <- directories)
            yield new DefaultResourceDescription(
                directory.prefix,
                new DefaultResourceProperties(true))
          
          dav.descriptions.render(rh.path, concat(of(root), concat(of(folders:_*), descriptions())))
        } else {
          // fetch resource properties
          dav.properties.render(rh.path, properties(resourceName(rh.path)))
        }
      } withHeaders(
         ("Content-Type", "application/xml;charset=utf-8"), 
         ("DAV", "1"))
    }
  } 
}