import java.net.MalformedURLException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.eclipse.jetty.server.{Request, Server}
import org.eclipse.jetty.server.handler._

import scala.util.{Failure, Success, Try}

abstract class JettyServer(port: Int, staticFilesUrl: String) {

  private val handlerOfStatic = {
    val handler = new ContextHandler("/")
    val resourceHandler = new ResourceHandler
    resourceHandler.setDirectoriesListed(true)
    resourceHandler.setResourceBase(staticFilesUrl)
    handler.setHandler(resourceHandler)
    handler
  }

  private val handlerOfApi = {
    val handlerWrapper = new ContextHandler("/api/")
    val handler = new AbstractHandler {
      override def handle(target: String,
                          baseRequest: Request,
                          request: HttpServletRequest,
                          response: HttpServletResponse) = {
        response.setContentType("application/json")
        response.setCharacterEncoding("UTF-8")

        route(request.getMethod, target, request.getParameter, response.setContentType) match {
          case Success(result) =>
            response.getWriter.write(result)
          case Failure(e: MalformedURLException) =>
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            response.getWriter.write(e.getMessage)
          case Failure(e) =>
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            response.getWriter.write(s"${e.getClass.getName}:  ${e.getMessage}")
        }

        baseRequest.setHandled(true)
      }
    }
    handlerWrapper.setHandler(handler)
    handlerWrapper
  }

  private val server = {
    val s = new Server(port)
    val handlers = new HandlerCollection
    handlers.setHandlers(Array(handlerOfStatic, handlerOfApi, new DefaultHandler))
    s.setHandler(handlers)
    s
  }

  def start() = {
    server.start()
    println(s"UI server started at localhost:$port")
  }

  def stop() = {
    server.stop()
    println("UI server stopped.")
  }

  def route(method: String, path: String, param: String => String, setContentType: String => Unit): Try[String]
}
