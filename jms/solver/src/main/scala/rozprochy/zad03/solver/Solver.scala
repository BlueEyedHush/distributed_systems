package rozprochy.zad03.solver

import javax.jms._

import org.slf4j.LoggerFactory
import rozprochy.zad03.common.{JmsUtils, JndiUtils, Utils}

/**
  * Created by blueeyedhush on 07.04.16.
  */
object Solver {
  private val LOGGER = LoggerFactory.getLogger(Solver.getClass.getSimpleName)

  def main(args: Array[String]): Unit = new Solver().mainLogic()
}

class Solver private () {

  private def mainLogic(): Unit = {
    val jndiUtils = new JndiUtils()
    val resultDestinations = buildDestinations(jndiUtils)
      .getOrElse(throw new RuntimeException("cannot build destination map"))
    val (consumer, producer) = initJms(jndiUtils).getOrElse(return)
    JmsUtils.setupTextMessageHandler(consumer,(m) => handleIncomingMessage(m, r => {
      producer.send(resultDestinations(r.exprType), r.result.toString)
    }))
    Utils.waitForQ()
  }

  private def initJms(jndiUtils: JndiUtils): Option[(JMSConsumer, JMSProducer)] = {
    val ctx = jndiUtils.connectionFactory().getOrElse(return None).createContext()
    val producer = ctx.createProducer()
    val compQ = jndiUtils.queue(JmsUtils.COMPUTATIONS_QUEUE_NAME).getOrElse(return None)
    val consumer = ctx.createConsumer(compQ)
    Some((consumer, producer))
  }

  private def handleIncomingMessage(m: String, resultHandler: Result => Unit) = {
    ExpressionEvaluator.evaluateIfCorrect(m) match {
      case Some(x) =>
        Solver.LOGGER.info("evaluated {} to {}", m, x.result)
        resultHandler(x)
      case _ => Solver.LOGGER.error("cannot evaluate {}", m)
    }
  }

  private def buildDestinations(jndiUtils: JndiUtils): Option[Map[String, Topic]] = {
    val map = JmsUtils.SUPPORTED_OPERATIONS
      .map(op => op -> jndiUtils.topic(JmsUtils.getTopicNameForOperation(op).getOrElse(return None)).getOrElse(return None))
      .toMap
    Some(map)
  }
}
