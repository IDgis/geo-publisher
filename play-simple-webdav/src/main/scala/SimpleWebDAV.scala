package nl.idgis.dav.router

import play.api.routing._
import play.api.mvc._
import play.mvc.Http.{Context => JContext, Request => JRequest, RequestImpl => JRequestImpl}

import nl.idgis.dav.xml
import nl.idgis.dav.model._

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
  @throws[Exception]
  def descriptions(): JStream[ResourceDescription] = emptyJStream()
  
  /** Called to fetch the properties of a resource. */
  @throws[Exception]
  def properties(name: String): JOptional[ResourceProperties] = emptyJOptional()
  
  /** Called to fetch a resource. */
  @throws[Exception]
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
  
  // create and set JContext in order to make Controller.request() work
  def ActionWithJContext(rh: RequestHeader)(block: => Result) = Action {
      JContext.current.set(new JContext(new JRequestImpl(rh)))
      block
  }
  
  def resources(depth: String, namePrefix: String = ""): Seq[ResourceDescription] = {
    val root = new DefaultResourceDescription(namePrefix, new DefaultResourceProperties(true))
    
    if(depth == "0") {
      Seq(root)
    } else {
      Seq(root) ++ 
        (if(depth == "1") {
          directories.map(directory =>
            new DefaultResourceDescription(
              directory.prefix.substring(prefix.length()),
              new DefaultResourceProperties(true)))
        } else {
          directories.flatMap(directory => directory.resources(depth, 
              namePrefix + directory.prefix.substring(prefix.length())))
        }) ++ 
          descriptions().iterator().map(description => 
            new DefaultResourceDescription(
                namePrefix + description.name(), 
                description.properties()))
    }
  }
  
  def folderRoutes: Router.Routes = {
    // fetch resource
    case rh: RequestHeader if rh.method == "GET"
        && rh.path.length() != prefix.length()
        && rh.path.startsWith(prefix) => ActionWithJContext(rh) {
          
        val r = resource(resourceName(rh.path))
        if(r.isPresent()) {
          Results.Ok(r.get.content) withHeaders(("Content-Type", r.get.contentType))
        } else {
          Results.NotFound("404 Not Found: " + rh.path)
        }
    }
    
    case rh: RequestHeader if rh.method == "PROPFIND" 
        && rh.path.startsWith(prefix) => ActionWithJContext(rh) {
          
      val depth = rh.headers.get("Depth").getOrElse("1")
      if(depth == "0" || depth == "1" || depth == "infinity") {
        Results.MultiStatus {
          if(rh.path == prefix) {
            // list all resources in folder
            xml.descriptions.render(rh.path, of(resources(depth):_*))
          } else {
            // fetch resource properties
            xml.properties.render(rh.path, properties(resourceName(rh.path)))
          }
        } withHeaders(
           ("Content-Type", "application/xml;charset=utf-8"), 
           ("DAV", "1"))
      } else {
        Results.InternalServerError
      }
    }
  } 
}