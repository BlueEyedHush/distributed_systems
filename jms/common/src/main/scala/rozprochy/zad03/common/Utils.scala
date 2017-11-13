package rozprochy.zad03.common

import scala.io.StdIn

/**
  * Created by blueeyedhush on 11.04.16.
  */
object Utils {
  def waitForQ() = {
    println("type q to exit")
    var stop = false
    while(!stop) stop = StdIn.readLine().trim.equals("q")
  }

  def re(msg: String): RuntimeException = new RuntimeException(msg)
}
