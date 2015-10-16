package cakesolutions.syntax

import com.codecommit.gll.RegexParsers

object QueryParser extends RegexParsers {

  lazy val GroundFact: Parser[QueryLanguage.GroundFact] =
    "[_a-zA-Z][_a-zA-Z0-9]*".r.filter(f => !QueryLanguage.keywords.contains(f)) ^^ QueryLanguage.GroundFact

  lazy val Proposition: Parser[QueryLanguage.Proposition] =
    "true" ^^ (_ => QueryLanguage.True) |
      "~" ~> "true" ^^ (_ => QueryLanguage.False) |
      "false" ^^ (_ => QueryLanguage.False) |
      "~" ~> "false" ^^ (_ => QueryLanguage.True) |
      GroundFact ^^ QueryLanguage.Assert |
      "~" ~> GroundFact ^^ (QueryLanguage.Neg andThen QueryLanguage.Assert) |
      Proposition ~ ("&&" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Conjunction(p, ps.head, ps.tail: _*) } |
      "~" ~> Proposition ~ ("&&" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Conjunction(QueryLanguage.not(p), ps.head, ps.tail: _*) } |
      Proposition ~ ("||" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Disjunction(p, ps.head, ps.tail: _*) } |
      "~" ~> Proposition ~ ("||" ~> Proposition).+ ^^ { case (p, ps) => QueryLanguage.Disjunction(QueryLanguage.not(p), ps.head, ps.tail: _*) } |
      "(" ~> Proposition <~ ")" ^^ identity |
      "~" ~> "(" ~> Proposition <~ ")" ^^ QueryLanguage.not

  // FIXME: deal with ambiguity!
  lazy val Path: Parser[QueryLanguage.Path] =
    Query <~ "?" ^^ QueryLanguage.Test |
      Path <~ "*" ^^ QueryLanguage.Repeat |
      Path ~ ("+" ~> Path).+ ^^ { case (p, ps) => QueryLanguage.Choice(p, ps.head, ps.tail: _*) } |
      Path ~ (";" ~> Path).+ ^^ { case (p, ps) => QueryLanguage.Sequence(p, ps.head, ps.tail: _*) } |
      "(" ~> Path <~ ")" ^^ identity |
      Proposition ^^ QueryLanguage.AssertFact

  // FIXME: deal with ambiguity!
  lazy val Query: Parser[QueryLanguage.Query] =
    "TT" ^^ (_ => QueryLanguage.TT) |
      "~" ~> "TT" ^^ (_ => QueryLanguage.FF) |
      "FF" ^^ (_ => QueryLanguage.FF) |
      "~" ~> "FF" ^^ (_ => QueryLanguage.TT) |
      "<" ~> Path ~ ">" ~ Query ^^ { case (p, _, q) => QueryLanguage.Exists(p, q) } |
      "~" ~> "<" ~> Path ~ ">" ~ Query ^^ { case (p, _, q) => QueryLanguage.All(p, QueryLanguage.not(q)) } |
      "[" ~> Path ~ "]" ~ Query ^^ { case (p, _, q) => QueryLanguage.All(p, q) } |
      "~" ~> "[" ~> Path ~ "]" ~ Query ^^ { case (p, _, q) => QueryLanguage.Exists(p, QueryLanguage.not(q)) } |
      Query ~ ("&&" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.And(q, qs.head, qs.tail: _*) } |
      "~" ~> Query ~ ("&&" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.And(QueryLanguage.not(q), qs.head, qs.tail: _*) } |
      Query ~ ("||" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.Or(q, qs.head, qs.tail: _*) } |
      "~" ~> Query ~ ("||" ~> Query).+ ^^ { case (q, qs) => QueryLanguage.Or(QueryLanguage.not(q), qs.head, qs.tail: _*) } |
      "(" ~> Query <~ ")" ^^ identity |
      "~" ~> "(" ~> Query <~ ")" ^^ QueryLanguage.not |
      Proposition ^^ QueryLanguage.Formula |
      "~" ~> Proposition ^^ (f => QueryLanguage.Formula(QueryLanguage.not(f)))

}
