package cakesolutions.model.provers

import cakesolutions.syntax.QueryLanguage

import scala.concurrent.{ExecutionContext, Future}

object Interface {

  import QueryLanguage._

  sealed trait ProverInterface
  case class Simplify(query: Query) extends ProverInterface
  case class Satisfiable(query: Query) extends ProverInterface
  case class Valid(query: Query) extends ProverInterface

}

trait Interface {

  import QueryLanguage._

  /**
   * Function that treats the query as a propositional formula (so, path expressions are taken to be "propositional")
   * and rewrites it by simplifying it.
   *
   * @param query query to be rewritten/simplified by applying propositional rules of reasoning
   */
  protected def simplify(query: Query)(implicit ec: ExecutionContext): Future[Query]

  /**
   * Function that interacts with an SMT prover and determines if the query is satisfiable or not.
   *
   * @param query LDL formula that is treated as being propositional
   */
  protected def satisfiable(query: Query)(implicit ec: ExecutionContext): Future[Boolean]

  /**
   * Function that interacts with an SMT prover and determines if the query is valid or not.
   *
   * @param query LDL formula that is treated as being propositional
   */
  protected def valid(query: Query)(implicit ec: ExecutionContext): Future[Boolean]

}