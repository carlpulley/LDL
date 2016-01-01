package cakesolutions.model.provers

import cakesolutions.syntax.QueryLanguage
import com.microsoft.z3._
import com.typesafe.config.Config

import scala.async.Async._
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class Z3(config: Config) extends Interface {

  import QueryLanguage._

  private[this] val ctx = new Context()
  private[this] val solver = ctx.mkSolver()
  private[this] var queryCount: Int = 0
  private[this] val queryMapping = mutable.HashMap.empty[Query, BoolExpr]
  private[this] var propCount: Int = 0
  private[this] val propMapping = mutable.HashMap.empty[Proposition, BoolExpr]

  private def propositionToExpr(fact: Proposition): BoolExpr = fact match {
    case True =>
      ctx.mkTrue()

    case False =>
      ctx.mkFalse()

    case prop: Assert =>
      if (!propMapping.contains(prop)) {
        // Ensure both +ve and -ve forms of sensor assertion are represented by propositions
        val newProp = s"fact_$propCount"
        propCount += 1
        // Z3 distinguishes variables by their memory instance, so we store freshly generated propositions for latter usage
        propMapping += (prop -> ctx.mkBoolConst(newProp), not(prop) -> ctx.mkBoolConst(s"not_$newProp"))
        // "$newProp" != "not_$newProp"
        val eq = ctx.mkNot(ctx.mkEq(propMapping(prop), propMapping(not(prop))))
        solver.add(eq) // add relational fact to SMT theory base
      }
      propMapping(prop)

    case Conjunction(fact1, fact2, remaining @ _*) =>
      val and = (fact1 +: fact2 +: remaining).map(propositionToExpr)
      ctx.mkAnd(and: _*)

    case Disjunction(fact1, fact2, remaining @ _*) =>
      val or = (fact1 +: fact2 +: remaining).map(propositionToExpr)
      ctx.mkOr(or: _*)
  }

  private def queryToExpr(query: Query)(implicit ec: ExecutionContext): BoolExpr = query match {
    case Formula(fact) =>
      propositionToExpr(fact)

    case TT =>
      ctx.mkBool(true)

    case FF =>
      ctx.mkBool(false)

    case And(query1, query2, remaining @ _*) =>
      val and = (query1 +: query2 +: remaining).map(queryToExpr)
      ctx.mkAnd(and: _*)

    case Or(query1, query2, remaining @ _*) =>
      val or = (query1 +: query2 +: remaining).map(queryToExpr)
      ctx.mkOr(or: _*)

    case _: Query =>
      // We are dealing with a path quantified LDL formula
      if (!queryMapping.contains(query)) {
        // Ensure both +ve and -ve forms of LDL formula are represented by propositions
        val newProp = s"query_$queryCount"
        queryCount += 1
        // Z3 distinguishes variables by their memory instance, so we store freshly generated propositions for latter usage
        queryMapping += (query -> ctx.mkBoolConst(newProp), not(query) -> ctx.mkBoolConst(s"not_$newProp"))
        // "$newProp" != "not_$newProp"
        val eq = ctx.mkNot(ctx.mkEq(queryMapping(query), queryMapping(not(query))))
        solver.add(eq) // add relational fact to SMT theory base
      }
      queryMapping(query)
  }

  private def exprToQuery(expr: BoolExpr)(implicit ec: ExecutionContext): Future[Query] = {
    if (expr.isTrue) {
      Future.successful(TT)
    } else if (expr.isFalse) {
      Future.successful(FF)
    } else if (expr.isConst) {
      val query = queryMapping.find(_._2.toString == expr.toString).map(_._1)
      val prop = propMapping.find(_._2.toString == expr.toString).map(_._1)

      if (query.nonEmpty) {
        Future.successful(query.get)
      } else if (prop.nonEmpty) {
        Future.successful(Formula(prop.get))
      } else {
        Future.failed(new RuntimeException(s"No propositional mapping exists for expression $expr"))
      }
    } else if (expr.isAnd) {
      if (expr.getNumArgs < 2) {
        Future.failed(new RuntimeException(s"And expression does not have enough arguments: $expr"))
      } else {
        async {
          val query1 = await(exprToQuery(expr.getArgs()(0).asInstanceOf[BoolExpr]))
          val query2 = await(exprToQuery(expr.getArgs()(1).asInstanceOf[BoolExpr]))
          val remaining = await(Future.sequence((2 until expr.getNumArgs).map(n => exprToQuery(expr.getArgs()(n).asInstanceOf[BoolExpr])))).toSeq

          And(query1, query2, remaining: _*)
        }
      }
    } else if (expr.isOr) {
      if (expr.getNumArgs < 2) {
        Future.failed(new RuntimeException(s"Or expression does not have enough arguments: $expr"))
      } else {
        async {
          val query1 = await(exprToQuery(expr.getArgs()(0).asInstanceOf[BoolExpr]))
          val query2 = await(exprToQuery(expr.getArgs()(1).asInstanceOf[BoolExpr]))
          val remaining = await(Future.sequence((2 until expr.getNumArgs).map(n => exprToQuery(expr.getArgs()(n).asInstanceOf[BoolExpr])))).toSeq

          Or(query1, query2, remaining: _*)
        }
      }
    } else {
      Future.failed(new RuntimeException(s"Unrecognised expression kind: $expr"))
    }
  }

  def simplify(query: Query)(implicit ec: ExecutionContext): Future[Query] = {
    exprToQuery(queryToExpr(query).simplify().asInstanceOf[BoolExpr])
  }

  def satisfiable(query: Query)(implicit ec: ExecutionContext): Future[Boolean] = {
    solver.add(queryToExpr(query))

    solver.check() match {
      case Status.SATISFIABLE =>
        Future.successful(true)

      case Status.UNSATISFIABLE =>
        Future.successful(false)

      case Status.UNKNOWN =>
        Future.failed(new RuntimeException(s"Failed to determine if $query was satisfiable or not - reason: ${solver.getReasonUnknown}"))
    }
  }

  def valid(query: Query)(implicit ec: ExecutionContext): Future[Boolean] = {
    solver.add(ctx.mkNot(queryToExpr(query)))

    solver.check() match {
      case Status.SATISFIABLE =>
        Future.successful(false)

      case Status.UNSATISFIABLE =>
        Future.successful(true)

      case Status.UNKNOWN =>
        Future.failed(new RuntimeException(s"Failed to determine if $query was valid or not - reason: ${solver.getReasonUnknown}"))
    }
  }

  def reset(): Unit = {
    queryCount = 0
    queryMapping.clear()
    propCount = 0
    propMapping.clear()
    solver.reset()
  }

  def statistics: Map[String, String] = {
    solver.getStatistics.getEntries.map(stat => (stat.Key, stat.getValueString)).toMap
  }

}
