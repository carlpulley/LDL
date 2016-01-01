package cakesolutions.benchmark

import java.util.concurrent.{Executors, TimeUnit}

import cakesolutions.model.ModelGenerators
import cakesolutions.model.provers.Z3
import cakesolutions.syntax.QueryLanguage.Query
import com.typesafe.config.ConfigFactory
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.concurrent.ExecutionContext

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.All))
@Fork(1)
@Threads(1)
@Warmup(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 20)
class Z3Benchmark extends ModelGenerators {

  val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  var z3: Option[Z3] = None

  var query: Option[Query] = None

  @Setup(Level.Trial)
  def setup(): Unit = {
    z3 = Some(new Z3(ConfigFactory.load("prover.conf")))
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    z3 = None
  }

  @Setup(Level.Invocation)
  def before(): Unit = {
    // TODO: move to using a tabular generator - for repeatable/reliable benchmarking
    query = QueryGen().sample
  }

  @TearDown(Level.Invocation)
  def after(): Unit = {
    query = None
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testValidity(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => query.map(p.valid(_)(ec))))
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testSatisfiability(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => query.map(p.satisfiable(_)(ec))))
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testSimplify(bh: Blackhole): Unit = {
    bh.consume(z3.flatMap(p => query.map(p.simplify(_)(ec))))
  }

}
