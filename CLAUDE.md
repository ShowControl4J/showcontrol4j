# CLAUDE.md

Guidance for anyone — human or AI agent — contributing to ShowControl4J,
particularly using agentic/AI-assisted coding workflows.

**Agentic contributions are welcome.** There is no requirement that a human
hand-type every line. The bar is the same either way: **fully tested and
stable before a PR is opened.** Generated code that hasn't been built, run,
and verified locally is not an acceptable PR, agent-written or not. See the
"Agentic contribution disclosure" section of the PR template — check it
honestly.

## Start here

Read [`ROADMAP.md`](./ROADMAP.md) before making any non-trivial change. It
holds the current architecture decisions, why they were made, and what phase
of the project this is. If you make a decision significant enough that a
future contributor (or agent) would need to know about it to avoid redoing
or reversing your work, **add an entry to the Decision Log in `ROADMAP.md`
in the same PR.** Don't let context live only in a chat transcript or a
closed PR discussion.

## Project shape

Three Maven modules, plain `org.showcontrol4j.*` packages:

- **`showcontrol4j-core`** — broker connection wrapper, message exchange,
  the `SCFJMessage`/`Instruction`/`ShowCommand` wire format. Everything else
  depends on this module. (Migrating RabbitMQ → MQTT here — see roadmap
  Phase 1; expect the broker/exchange classes to be in flux.)
- **`showcontrol4j-element`** — the receiving side. `ShowElement` is the
  abstract base (state machine: idle loop / show loop / shutdown, driven by
  messages from the broker). Concrete elements (e.g.
  `raspberrypi.GeneralPurposeIOShowElement`) extend it for specific hardware.
- **`showcontrol4j-trigger`** — the sending side. `ShowTrigger` is the
  abstract base; concrete triggers (e.g. `keyboard.KeyboardShowTrigger`)
  publish `GO`/`IDLE`/`SHUTDOWN` commands to the exchange.

New trigger/element types are added by extending the relevant abstract base
class, not by modifying it, unless the base contract itself is what's
changing.

## Build & test

```
mvn clean install          # build all modules, run unit tests
mvn -pl showcontrol4j-core test   # test a single module
```

Tests are JUnit 4 + Mockito (inline mock maker — see
`showcontrol4j-trigger/src/test/resources/mockito-extensions/`). Every
non-trivial class has a corresponding test class today; keep that pattern.

Code that talks to real hardware (GPIO, a physical broker) generally can't
be fully exercised in CI. Mock what you can (broker connections, GPIO pin
interfaces) and say explicitly in the PR what was and wasn't verified
against real hardware — see the PR template's "How has it been tested?"
section. Don't claim hardware-level testing you didn't do.

## Conventions already in use — follow them

- Lombok for boilerplate (`@Getter`/`@Setter`/`@Builder`/`@Data`/`@Slf4j`),
  except where a class needs custom construction logic Lombok can't express
  (see `BrokerConnectionFactory`, which is deliberately hand-written).
- Builder pattern for message/config objects.
- `final` on parameters and fields wherever mutability isn't required.
- Javadoc on public classes and methods, written for library consumers, not
  as a restatement of the method name.
- Abstract base + concrete-subclass pattern for anything hardware- or
  transport-specific (see `ShowElement`/`ShowTrigger`).

## Branching and PRs

- Branch names: `jhare/short-description-of-task` — no ticket, task, or
  epic numbers in the branch name.
- One squashed commit per branch before it's merged.
- PRs are opened by the repo owner, not by an agent, unless explicitly told
  otherwise for a given task.
- Use `.github/pull_request_template.md` — fill in what the change
  accomplishes and exactly how it was tested. "Tests pass" alone is not a
  sufficient answer for anything touching timing, hardware I/O, or the
  broker.

## What not to do

- Don't widen a PR's scope beyond what the roadmap item or issue asked for.
- Don't introduce a new messaging protocol, dependency, or architectural
  pattern without an entry in the `ROADMAP.md` Decision Log explaining why —
  if it's not decided there, raise it as a question rather than assuming.
- Don't submit hardware-facing code (GPIO, sensor triggers) that's only been
  compiled, not run — say so instead if a physical device wasn't available.
