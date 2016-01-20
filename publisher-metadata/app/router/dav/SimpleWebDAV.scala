package router.dav

import play.api.routing._
import play.api.mvc._

import views.xml.dav

import model.dav._

/** Simple read-only WebDAV implementation.
  *  
  * This router provides a minimal WebDAV implementation for creating a
  * single read-only WebDAV folder.
  */
abstract class SimpleWebDAV(val prefix: String) extends Router {
  require(prefix.endsWith("/"), "WebDAV url should have a trailing '/'")
  
  // prepare documentation lines,
  // these lines are shown when no action is found
  val methodPrefix = getClass().getCanonicalName() + ".";
  
  def documentation: Seq[(String, String, String)] = List(
     ("PROPFIND", prefix, methodPrefix + "descriptions"),
     ("PROPFIND", prefix + "*", methodPrefix + "properties"),
     ("GET", prefix + "*", methodPrefix + "resource"))
  
  // no argument constructor is required to include
  // this router in static routes
  def this() = this("/")
  
  /** Called to fetch a list of resources in the WebDAV folder. */  
  def descriptions(): java.util.stream.Stream[ResourceDescription]
  
  /** Called to fetch the properties of a resource. */
  def properties(name: String): java.util.Optional[ResourceProperties]
  
  /** Called to fetch a resource. */
  def resource(name: String): java.util.Optional[Resource]
  
  private def resourceName(path :String) = path.substring(prefix.length())
  
  override def routes = {
    // fetch resource
    case rh: RequestHeader if rh.method == "GET"
        && rh.path.length() != prefix.length()
        && rh.path.startsWith(prefix) => Action {
          
        val r = resource(resourceName(rh.path))
        if(r.isPresent()) {
          Results.Ok(r.get.content) withHeaders(("Content-Type", r.get.contentType))
        } else {
          Results.NotFound
        }
    }
    
    case rh: RequestHeader if rh.method == "PROPFIND" 
        && rh.path.startsWith(prefix) => Action {
      
      Results.Status(207) {
        if(rh.path == prefix) {
          // list all resources in folder
          dav.descriptions.render(rh.path, descriptions())
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