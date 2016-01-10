package cakesolutions.monitor

import akka.actor._
import cakesolutions.syntax.QueryLanguage

import scala.reflect.ClassTag
import scala.util.{Success, Try}

object RuntimeMonitor {

  type Location = ActorRef

  sealed trait Proposition {
    def toProposition: QueryLanguage.Proposition
  }
  // TODO: tweak this mapping
  case class fact[Language : Manifest]() extends Proposition {
    override def toProposition: QueryLanguage.Proposition =
      QueryLanguage.Assert(QueryLanguage.GroundFact(manifest[Language].runtimeClass.getName))
  }
  // TODO: tweak this mapping
  case class notFact[Language : Manifest]() extends Proposition {
    override def toProposition: QueryLanguage.Proposition =
      QueryLanguage.Assert(QueryLanguage.Neg(QueryLanguage.GroundFact(manifest[Language].runtimeClass.getName)))
  }
  case class conj(props: Proposition*) extends Proposition {
    override def toProposition: QueryLanguage.Proposition = props.length match {
      case 0 =>
        QueryLanguage.True

      case 1 =>
        props.head.toProposition

      case _: Int =>
        val qlProps = props.map(_.toProposition)
        QueryLanguage.Conjunction(qlProps.head, qlProps(1), qlProps.drop(2): _*)
    }
  }
  case class disj(props: Proposition*) extends Proposition {
    override def toProposition: QueryLanguage.Proposition = props.length match {
      case 0 =>
        QueryLanguage.False

      case 1 =>
        props.head.toProposition

      case _: Int =>
        val qlProps = props.toSeq.map(_.toProposition)
       QueryLanguage.Disjunction(qlProps.head, qlProps(1), qlProps.drop(2): _*)
    }
  }

  sealed trait Path {
    def `;`(that: Path): Path = {
      sequence(this, that)
    }

    def `+`(that: Path): Path = {
      sum(this, that)
    }

    def `*`(): Path = {
      repeat(this)
    }

    def toPath: QueryLanguage.Path
  }
  // FIXME: add in location and type
  case class `!`(locn: Location, prop: Proposition) extends Path {
    override def toPath: QueryLanguage.Path =
      QueryLanguage.AssertFact(prop.toProposition)
  }
  // FIXME: add in location and type
  case class `?`(locn: Location, prop: Proposition) extends Path {
    override def toPath: QueryLanguage.Path =
      QueryLanguage.AssertFact(QueryLanguage.not(prop.toProposition))
  }
  case class check(query: Query) extends Path {
    override def toPath: QueryLanguage.Path =
      QueryLanguage.Test(query.toQuery)
  }
  case class sequence(paths: Path*) extends Path {
    override def toPath: QueryLanguage.Path = paths.length match {
      case 0 =>
        QueryLanguage.AssertFact(QueryLanguage.True)

      case 1 =>
        paths.head.toPath

      case _: Int =>
        val qlPaths = paths.map(_.toPath)
        QueryLanguage.Sequence(qlPaths.head, qlPaths(1), qlPaths.drop(2): _*)
    }
  }
  case class sum(paths: Path*) extends Path {
    override def toPath: QueryLanguage.Path = paths.length match {
      case 0 =>
        QueryLanguage.AssertFact(QueryLanguage.False)

      case 1 =>
        paths.head.toPath

      case _: Int =>
        val qlPaths = paths.map(_.toPath)
        QueryLanguage.Choice(qlPaths.head, qlPaths(1), qlPaths.drop(2): _*)
    }
  }
  case class repeat(path: Path) extends Path {
    override def toPath: QueryLanguage.Path =
      QueryLanguage.Repeat(path.toPath)
  }

  sealed trait Query {
    def `&&`(that: Query): Query = {
      and(this, that)
    }

    def `||`(that: Query): Query = {
      or(this, that)
    }

    def toQuery: QueryLanguage.Query
  }
  case class state(prop: Proposition) extends Query {
    override def toQuery: QueryLanguage.Query =
      QueryLanguage.Formula(prop.toProposition)
  }
  case class and(queries: Query*) extends Query {
    override def toQuery: QueryLanguage.Query = queries.length match {
      case 0 =>
        QueryLanguage.TT

      case 1 =>
        queries.head.toQuery

      case _: Int =>
        val qlQueries = queries.map(_.toQuery)
        QueryLanguage.And(qlQueries.head, qlQueries(1), qlQueries.drop(2): _*)
    }
  }
  case class or(queries: Query*) extends Query {
    override def toQuery: QueryLanguage.Query = queries.length match {
      case 0 =>
        QueryLanguage.FF

      case 1 =>
        queries.head.toQuery

      case _: Int =>
        val qlQueries = queries.map(_.toQuery)
        QueryLanguage.Or(qlQueries.head, qlQueries(1), qlQueries.drop(2): _*)
    }
  }
  case class every(path: Path, query: Query) extends Query {
    override def toQuery: QueryLanguage.Query =
      QueryLanguage.All(path.toPath, query.toQuery)
  }
  case class some(path: Path, query: Query) extends Query {
    override def toQuery: QueryLanguage.Query =
      QueryLanguage.Exists(path.toPath, query.toQuery)
  }

}

class RuntimeMonitor(system: ExtendedActorSystem) extends Extension {

  def actorOf[T : ClassTag](props: Props)(sessionType: RuntimeMonitor.Query): Try[ActorRef] = {
    Setup.checker(sessionType).map { ref =>
      Setup.probe[T](props, ref)(sessionType)
    }
  }

  def actorOf[T : ClassTag](props: Props, name: String)(sessionType: RuntimeMonitor.Query): Try[ActorRef] = {
    Setup.checker(sessionType).map { ref =>
      Setup.probe[T](props, ref, name)(sessionType)
    }
  }

  object Setup {

    def probe[T : ClassTag](props: Props, monitor: ActorRef)(sessionType: RuntimeMonitor.Query): ActorRef =
      system.actorOf(Probe.props[T](props, monitor, sessionType))

    def probe[T : ClassTag](props: Props, monitor: ActorRef, name: String)(sessionType: RuntimeMonitor.Query): ActorRef =
      system.actorOf(Probe.props[T](props, monitor, sessionType), name)

    def checker(sessionType: RuntimeMonitor.Query): Try[ActorRef] = {
      Success(system.actorOf(Model.props(sessionType)))
    }

  }

}
