# Real-time Akka Monitoring using Linear Dynamic Logic and Session Types

Session types for Akka are specified using Linear Dynamic Logic. From these session type specifications, the Akka monitoring extension compiles
queries that define a real-time monitor for the wrapped Akka actor.

The real-time monitor is implemented using Linear Dynamic Logic, with validity and satisfiability implemented using SMT provers. Both Z3 and CVC4 SMT prover
interfaces are defined.

# Release History

[release 0.0.2](https://github.com/carlpulley/LDL/releases/tag/0.0.2) Propositional session type syntax; actor instrumentation; notification/monitor interface; CVC4 and Z3 provers; benchmarking framework.

[release 0.0.1](https://github.com/carlpulley/LDL/releases/tag/0.0.1) This code originated from [Muvr](https://github.com/muvr/open-muvr)'s `exercise-query` module.
