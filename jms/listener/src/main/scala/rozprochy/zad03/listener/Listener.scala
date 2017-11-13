package rozprochy.zad03.listener

import rozprochy.zad03.common.{JmsUtils, JndiUtils, Utils}

/**
  * Created by blueeyedhush on 11.04.16.
  */
object Listener {
  def main(args: Array[String]) {
    val operatorToListenFor = parseInput(args).getOrElse(throw new RuntimeException("incorrect operator"))
    val jndiUtils = new JndiUtils()
    val topic = JmsUtils.getTopicForOperation(operatorToListenFor, jndiUtils)
      .getOrElse(throw new RuntimeException("cannot lookup topic"))

    val consumer = jndiUtils.connectionFactory()
      .map(_.createContext().createConsumer(topic))
      .getOrElse(throw new RuntimeException("cannot create consumer"))

    JmsUtils.setupTextMessageHandler(consumer, m => println("[%s result]: %s".format(operatorToListenFor, m)))
    Utils.waitForQ()
  }

  private def parseInput(args: Array[String]): Option[String] = {
    if(args.length < 1) None
    else {
      val firstArg = args(0).trim
      if(JmsUtils.SUPPORTED_OPERATIONS.contains(firstArg)) return Some(firstArg)
      else None
    }
  }
}
