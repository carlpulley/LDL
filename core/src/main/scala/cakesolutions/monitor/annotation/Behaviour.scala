package cakesolutions.monitor.annotation

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

private object Behaviour {

  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val annotation = this.getClass.getName

    annottees.map(_.tree).toList match {
      case (expr @ q"$mods class $name[..$tparams](..$first)(...$rest) extends ..$parents { $self => ..$body }") :: tail =>
        expr match {
          case q"$mods class $tname[..$tparams] $ctorMods (...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$body }" =>
            val query = c.prefix.tree match {
              case q"new Behaviour($arg)" =>
                c.Expr(arg)
            }
            val traitName = TypeName(c.freshName("ReceiveHook"))

            // TODO: add in correlation between trapped messages and prover notifications
            c.Expr(
              q"""
              trait $traitName extends akka.contrib.pattern.ReceivePipeline {
                private[this] val behaviour = cakesolutions.syntax.BehaviourParser.behaviour($query).get

                implicit val monitor = cakesolutions.monitor.Model.Monitor(context.system.actorOf(cakesolutions.monitor.Model.props(self, behaviour)))

                override def postStop(): Unit = {
                  monitor.ref ! cakesolutions.monitor.Model.Completed
                  super.postStop()
                }

                def role(observations: String*): Unit = {
                  monitor.ref ! cakesolutions.monitor.Model.State(observations.map(obs => cakesolutions.syntax.QueryLanguage.GroundFact(obs, Some("role"))).toSet)
                }

                pipelineOuter {
                  case cakesolutions.model.QueryModel.StableValue(false) =>
                    context.system.log.error("Monitor has falsified query - stopping actor!")
                    context.stop(self)
                    akka.contrib.pattern.ReceivePipeline.HandledCompletely

                  case _: cakesolutions.model.QueryModel.Notification =>
                    akka.contrib.pattern.ReceivePipeline.HandledCompletely

                  case msg: Any =>
                    try {
                      val realSender = sender()
                      monitor.ref ! cakesolutions.monitor.Model.Receive(msg, realSender)
                      akka.contrib.pattern.ReceivePipeline.Inner(msg)
                    } catch {
                      case scala.util.control.NonFatal(exn) =>
                        context.system.log.error(exn, "Failed to forward received message to monitor - stopping actor!")
                        context.stop(self)
                        akka.contrib.pattern.ReceivePipeline.HandledCompletely
                    }
                }
              }

              $mods class $tname[..$tparams] $ctorMods (...$paramss) extends { ..$earlydefns } with ..$parents with $traitName { $self => ..$body }
            """
            )

          case tree =>
            c.abort(c.enclosingPosition, s"$annotation matching error with $tree")
        }
    }
  }

}

@compileTimeOnly("Enable macro paradise to expand @Behaviour macro annotations")
final class Behaviour(query: String) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro Behaviour.impl

}
