package rozprochy.zad03.common

import javax.jms._

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by blueeyedhush on 06.04.16.
  */
object JmsUtils {
  val COMPUTATIONS_QUEUE_NAME = "computations"
  private val RESULT_T_PREFIX = "results"

  private val LOGGER = LoggerFactory.getLogger(JmsUtils.getClass.getSimpleName)

  def getTopicNameForOperation(op: String): String = RESULT_T_PREFIX + op

  def setupTextMessageHandler(c: JMSConsumer, messageHandler: String => Unit) = {
    c.setMessageListener((m: Message) => {
      Success(m)
        .map(_.asInstanceOf[TextMessage].getText())
        .recoverWith{case x =>
          LOGGER.warn("unsupported message: " + m.toString)
          Failure(x)
        }
        .foreach(msg => messageHandler(msg))
    })
  }
}
