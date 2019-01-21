package services

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import model.{CheckQuery, RuleMatch}

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait CategoryHandler {
  def check(req: CheckQuery): Future[List[RuleMatch]]
  def shutdown(): Future[Unit]
}

trait NonThreadsafeLanguageTool {
  def check(req: CheckQuery): List[RuleMatch]
  def shutdown(): Unit
}

class LanguageToolCategoryHandler(factory: LanguageToolFactory) extends CategoryHandler {
  private val queue = new LinkedBlockingQueue[(CheckQuery, Promise[List[RuleMatch]])]()
  // Try to shutdown as soon as possible without processing the rest of the queue
  private val shutdownPromise = new AtomicReference[Option[Promise[Unit]]](None)

  override def check(req: CheckQuery): Future[List[RuleMatch]] = {
    val ret = Promise[List[RuleMatch]]()
    queue.add((req, ret))

    ret.future
  }

  override def shutdown(): Future[Unit] = {
    val ret = Promise[Unit]
    shutdownPromise.set(Some(ret))

    ret.future
  }

  new Thread(new LanguageToolHandlerThread(factory, queue, shutdownPromise), s"language-tool-${factory.getName}").start()
}

class LanguageToolHandlerThread(factory: LanguageToolFactory, queue: BlockingQueue[(CheckQuery, Promise[List[RuleMatch]])],
                                shutdownPromise: AtomicReference[Option[Promise[Unit]]]) extends Runnable {

  val languageTool = Try(factory.createInstance())

  override def run() = {
    while (shutdownPromise.get().isEmpty) {
      val (request, ret) = queue.take()

      languageTool match {
        case Success(tool) =>
          try {
            val response = tool.check(request)
            ret.success(response.toList)
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
          // Any shutdown logic goes here
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


