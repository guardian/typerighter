package services

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import model.{CheckQuery, RuleMatch}
import play.api.Logger

import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait InstanceHandler {
  def check(req: CheckQuery): Future[List[RuleMatch]]
  def shutdown(): Future[Unit]
}

class LanguageToolInstancePool(factory: LanguageToolFactory, noOfThreads: Int = 1) extends InstanceHandler {
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

  for {i <- 0 until noOfThreads} {
    val threadName = s"language-tool-${factory.getName}-$i"
    Logger.info(s"Creating new thread: $threadName")
    new Thread(new LanguageToolInstanceManager(factory, queue, shutdownPromise), threadName).start()
  }
}

class LanguageToolInstanceManager(factory: LanguageToolFactory, queue: BlockingQueue[(CheckQuery, Promise[List[RuleMatch]])],
                                  shutdownPromise: AtomicReference[Option[Promise[Unit]]]) extends Runnable {

  val languageTool = Try(factory.createInstance())

  override def run(): Unit = {
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


