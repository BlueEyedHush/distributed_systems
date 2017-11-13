import java.util.regex.Pattern
import javax.jms.{Destination, JMSProducer}

import org.slf4j.LoggerFactory
import rozprochy.zad03.common.{Config, JmsUtils, JndiUtils}

import scala.util.{Success, Failure, Try}
import rozprochy.zad03.common.Utils.re

/**
  * Created by blueeyedhush on 05.04.16.
  */
object Producer {
  private val WHITESPACE_PATTERN = "\\h+"

  private val LOGGER = LoggerFactory.getLogger(Producer.getClass.getSimpleName)

  def main(args: Array[String]): Unit = mainLogic()

  private def mainLogic(): Unit = {
    val jndiUtils = new JndiUtils()
    val producer = initJms(jndiUtils)
        .recoverWith({
          case x => LOGGER.error("cannot create producer", x)
            return})
        .get

    val destination = jndiUtils.queue(JmsUtils.COMPUTATIONS_QUEUE_NAME)
        .recoverWith({case e => LOGGER.error("cannot get destination", e); Failure(e)})
        .get

    interactiveLoop((expr) => {
      Success(expr)
        .map(_.replaceAll(WHITESPACE_PATTERN, ""))
        .flatMap(ensureExprNonEmpty)
        .flatMap(verfyExpressionSupported)
        .fold(t => println(t.getMessage), producer.send(destination, _)) //getOrLeft
    })
  }

  private def initJms(utils: JndiUtils): Try[JMSProducer] =
    utils.connectionFactory().map(cf => cf.createContext().createProducer())

  private def interactiveLoop(onInput: (String) => Unit) = {
    var input = ""
    var stop = false
    while(!stop) {
      input = scala.io.StdIn.readLine("Type expression or q to exit: ")
      input.toLowerCase match {
        case "q" => stop = true
        case expr => onInput(expr)
      }
    }
  }

  private def ensureExprNonEmpty(expr: String): Try[String] =
    if(expr.isEmpty)
      Success(expr)
    else
      Failure(re("expression empty"))

  private def verfyExpressionSupported(expr: String): Try[String] =
    if(expr.length <= Config.EXPR_MAX_LEN)
      Success(expr)
    else
      Failure(re("expression too long"))

  private def handleExpr(expr: String, dest: Destination)(implicit prod: JMSProducer) = {
    LOGGER.info(expr)
    prod.send(dest, expr)
  }
}
