package utils

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class CategoryRequest()
case class CategoryResponse()

trait CategoryHandler {
  def doRequest(req: CategoryRequest): Future[CategoryResponse]
  def shutdown(): Future[Unit]
}

trait NonThreadsafeLanguageTool {
  def doRequest(req: CategoryRequest): CategoryResponse
  def shutdown(): Unit
}

trait LanguageToolFactory {
  def name: String
  def create(): NonThreadsafeLanguageTool
}

class LanguageToolCategoryHandler(factory: LanguageToolFactory) extends CategoryHandler {
  private val queue = new LinkedBlockingQueue[(CategoryRequest, Promise[CategoryResponse])]()
  // Try to shutdown as soon as possible without processing the rest of the queue
  private val shutdownPromise = new AtomicReference[Option[Promise[Unit]]](None)

  override def doRequest(req: CategoryRequest): Future[CategoryResponse] = {
    val ret = Promise[CategoryResponse]()
    queue.add((req, ret))

    ret.future
  }

  override def shutdown(): Future[Unit] = {
    val ret = Promise[Unit]
    shutdownPromise.set(Some(ret))

    ret.future
  }

  new Thread(new LanguageToolHandlerThread(factory, queue, shutdownPromise), s"language-tool-${factory.name}").start()
}

class LanguageToolHandlerThread(factory: LanguageToolFactory, queue: BlockingQueue[(CategoryRequest, Promise[CategoryResponse])],
                                shutdownPromise: AtomicReference[Option[Promise[Unit]]]) extends Runnable {

  val languageTool = Try(factory.create())

  override def run() = {
    while (shutdownPromise.get().isEmpty) {
      val (request, ret) = queue.take()

      languageTool match {
        case Success(tool) =>
          try {
            val response = tool.doRequest(request)
            ret.success(response)
          } catch {
            case NonFatal(err) =>
              ret.failure(err)
          }

        case Failure(err) =>
          ret.failure(err)
      }
    }

    // Shutdown
    languageTool match {
      case Success(tool) =>
        try {
          tool.shutdown()
          shutdownPromise.get().foreach(_.success(()))
        } catch {
          case NonFatal(err) =>
            shutdownPromise.get().foreach(_.failure(err))
        }

      case Failure(err) =>
        shutdownPromise.get().map(_.failure(err))
    }
  }
}


