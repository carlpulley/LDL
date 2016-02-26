package cakesolutions.model
package benchmark

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import cakesolutions.model.ModelGenerators
import cakesolutions.model.provers.Z3
import cakesolutions.syntax.QueryLanguage.Query

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.All))
@Fork(1)
@Threads(1)
@Warmup(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 20)
class Z3Benchmark extends ModelGenerators {

  var z3: Option[Z3] = None

  var behaviour: Option[Query] = None

  @Setup(Level.Trial)
  def setup(): Unit = {
    z3 = Some(new Z3(ConfigFactory.load("reference.conf")))
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    z3 = None
  }

  @Setup(Level.Invocation)
  def before(): Unit = {
    // TODO: move to using a tabular generator - for repeatable/reliable benchmarking
    behaviour = BehaviourGen().sample
  }

  @TearDown(Level.Invocation)
  def after(): Unit = {
    behaviour = None
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testValidity(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => behaviour.map(p.valid)))
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testSatisfiability(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => behaviour.map(p.satisfiable)))
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testSimplify(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => behaviour.map(p.simplify)))
  }

}