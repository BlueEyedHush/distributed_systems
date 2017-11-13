package rozprochy.zad03.solver

import java.util.regex.{Matcher, Pattern}

/**
  * Created by blueeyedhush on 07.04.16.
  */

case class Result(exprType: String, result: Long)

object ExpressionEvaluator {
  private val OPERAND_PATTERN = "([0-9]{1,4})"
  val OPERAND_TO_HANDLER = Map("+" -> ((m: Matcher) => op(1, m) + op(2, m)),
    "-" -> ((m: Matcher) => op(1, m) - op(2, m)))

  def evaluateIfCorrect(exprString: String): Option[Result] = {
    for((o, h) <- OPERAND_TO_HANDLER) {
      val m = Pattern.compile(OPERAND_PATTERN + Pattern.quote(o) + OPERAND_PATTERN).matcher(exprString)
      if(m.matches()) {
        return Some(new Result(o, h(m)))
      }
    }
    None
  }

  private def op(which: Int, matcher: Matcher): Int = matcher.group(which).toInt
}
