package cakesolutions.model.provers

import cakesolutions.syntax.QueryLanguage

import scala.util.Try

trait Interface {

  import QueryLanguage._

  /**
   * Function that treats the query as a propositional formula (so, path expressions are taken to be "propositional")
   * and rewrites it by simplifying it.
   *
   * @param query query to be rewritten/simplified by applying propositional rules of reasoning
   */
  def simplify(query: Query): Try[Query]

  /**
   * Function that interacts with an SMT prover and determines if the query is satisfiable or not.
   *
   * @param query LDL formula that is treated as being propositional
   */
  def satisfiable(query: Query): Try[Boolean]

  /**
   * Function that interacts with an SMT prover and determines if the query is valid or not.
   *
   * @param query LDL formula that is treated as being propositional
   */
  def valid(query: Query): Try[Boolean]

  /**
   * Function to reset underlying SMT prover.
   */
  def reset(): Unit

  /**
   * Function that returns prover related statistics.
   *
   * @return Map of labeled statistical values
   */
  def statistics: Map[String, String]

}
