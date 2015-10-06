package cakesolutions.model

import akka.actor.ActorRef
import cakesolutions.syntax.QueryLanguage

object QueryModel {

  import QueryLanguage._

  /**
   * Events wrap observations that the `ActorSystem` makes.
   *
   * `Next` events imply the event stream remains unclosed. `Completed` events will close the stream.
   */
  sealed trait Event
  sealed trait ObservableEvent extends Event {
    def observation: Observation
    def replyTo: ActorRef
  }
  case class Next(observation: Observation, replyTo: ActorRef) extends ObservableEvent
  case class Completed(observation: Observation, replyTo: ActorRef) extends ObservableEvent
  case object Cancel extends Event

  /**
   * Values representing the current evaluation state of a given query:
   *   - stable queries are values that hold now and, no matter how the model develops, will remain in their current state
   *   - unstable queries are values that hold now and, for some sequence of possible events updates, may deviate from
   *     their current value
   */
  sealed trait Notification
  /**
   * @param result validity of linear dynamic logic statement at this and future points in time
   */
  case class StableValue(result: Boolean) extends Notification
  /**
   * @param state  positive propositional description of the next states for an alternating automaton over words
   */
  case class UnstableValue(state: Query) extends Notification

  /**
   * Auxillary functions that support QueryValue lattice structure
   */

  def meet(value1: Notification, value2: Notification): Notification = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 && result2)

    case (UnstableValue(atom1), UnstableValue(atom2)) =>
      UnstableValue(And(atom1, atom2))

    case (StableValue(true), result2 @ UnstableValue(_)) =>
      result2

    case (result1 @ StableValue(false), UnstableValue(_)) =>
      result1

    case (result1 @ UnstableValue(_), StableValue(true)) =>
      result1

    case (UnstableValue(_), result2 @ StableValue(false)) =>
      result2
  }

  def join(value1: Notification, value2: Notification): Notification = (value1, value2) match {
    case (StableValue(result1), StableValue(result2)) =>
      StableValue(result1 || result2)

    case (UnstableValue(atom1), UnstableValue(atom2)) =>
      UnstableValue(Or(atom1, atom2))

    case (result1 @ StableValue(true), UnstableValue(_)) =>
      result1

    case (StableValue(false), result2 @ UnstableValue(_)) =>
      result2

    case (UnstableValue(_), result2 @ StableValue(true)) =>
      result2

    case (result1 @ UnstableValue(_), StableValue(false)) =>
      result1
  }

  def complement(value: Notification): Notification = value match {
    case StableValue(result) =>
      StableValue(!result)

    case UnstableValue(atom) =>
      UnstableValue(not(atom))
  }

}
