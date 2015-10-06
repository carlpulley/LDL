package cakesolutions.syntax

import com.codecommit.gll.RegexParsers

object QueryParser extends RegexParsers {

  lazy val GroundFact: Parser[QueryLanguage.GroundFact] =
    "[_a-zA-Z][_a-zA-Z0-9]*".r.filter(f => !QueryLanguage.keywords.contains(f)) ^^ QueryLanguage.GroundFact

  // FIXME: enable unambiguous use of not!
  lazy val Proposition: Parser[QueryLanguage.Proposition] =
    "true" ^^ (_ => QueryLanguage.True) |
      "false" ^^ (_ => QueryLanguage.False) |
      GroundFact ^^ QueryLanguage.Assert |
      "~" ~> GroundFact ^^ (QueryLanguage.Neg andThen QueryLanguage.Assert) |
      //"~" ~> Proposition ^^ QueryLanguage.not |
      Proposition ~ ("&&" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Conjunction(p, ps.head, ps.tail: _*) } |
      Proposition ~ ("||" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Disjunction(p, ps.head, ps.tail: _*) } |
      "(" ~> Proposition <~ ")" ^^ identity

  // FIXME: deal with ambiguity!
  lazy val Path: Parser[QueryLanguage.Path] =
    Query <~ "?" ^^ QueryLanguage.Test |
      Path <~ "*" ^^ QueryLanguage.Repeat |
      Path ~ ("+" ~> Path).+ ^^ { case (p, ps) => QueryLanguage.Choice(p, ps.head, ps.tail: _*) } |
      Path ~ (";" ~> Path).+ ^^ { case (p, ps) => QueryLanguage.Sequence(p, ps.head, ps.tail: _*) } |
      "(" ~> Path <~ ")" ^^ identity |
      Proposition ^^ QueryLanguage.AssertFact

  // FIXME: deal with ambiguity!
  // FIXME: enable unambiguous use of not!
  lazy val Query: Parser[QueryLanguage.Query] =
    "TT" ^^ (_ => QueryLanguage.TT) |
      "FF" ^^ (_ => QueryLanguage.FF) |
      //"~" ~> Query ^^ QueryLanguage.not |
      "<" ~> Path ~ ">" ~ Query ^^ { case (p, _, q) => QueryLanguage.Exists(p, q) } |
      "[" ~> Path ~ "]" ~ Query ^^ { case (p, _, q) => QueryLanguage.All(p, q) } |
      Query ~ ("&&" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.And(q, qs.head, qs.tail: _*) } |
      Query ~ ("||" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.Or(q, qs.head, qs.tail: _*) } |
      "(" ~> Query <~ ")" ^^ identity |
      Proposition ^^ QueryLanguage.Formula

}
