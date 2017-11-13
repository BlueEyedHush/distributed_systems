package rozprochy.zad03.common

import java.util
import javax.jms.{ConnectionFactory, Queue, Topic}
import javax.naming.{Context, InitialContext, NamingException}

import org.slf4j.LoggerFactory

import scala.util.{Success, Try}

/**
  * Created by blueeyedhush on 06.04.16.
  */
object JndiUtils {
  val PROVIDER_HOST = "127.0.0.1"
  val PROVIDER_PORT = "61616"
  private val CONNECTION_FACTORY_KEY = "connectionFactory.ConnectionFactory"
  private val CONNECTION_FACTORY_LOOKUP_NAME = "ConnectionFactory"
  private val ARTEMIS_QPREFIX = "dynamicQueues/"
  private val ARTEMIS_TPREFIX = "dynamicTopics/"

  private val LOGGER = LoggerFactory.getLogger(JndiUtils.getClass.getSimpleName)

  private def jndiContext(): InitialContext = {
    val contextEnvironment = new util.Hashtable[String, String](2)
    contextEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory")
    contextEnvironment.put(CONNECTION_FACTORY_KEY, "tcp://%s:%s".format(PROVIDER_HOST, PROVIDER_PORT))
    new InitialContext(contextEnvironment)
  }
}

class JndiUtils {
  import rozprochy.zad03.common.JndiUtils._

  private val jndiCtx = jndiContext()

  def connectionFactory(): Try[ConnectionFactory] = {
    Success(jndiContext())
      .map(_.lookup(CONNECTION_FACTORY_LOOKUP_NAME))
      .filter(_.isInstanceOf[ConnectionFactory])
      .map(_.asInstanceOf[ConnectionFactory])
  }

  def queue(name: String): Try[Queue] = Success(name)
    .map(n => jndiCtx.lookup(ARTEMIS_QPREFIX + n).asInstanceOf[Queue])

  def topic(name: String): Try[Topic] = Success(name)
    .map(n => jndiCtx.lookup(ARTEMIS_TPREFIX + n).asInstanceOf[Topic])
}