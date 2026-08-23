# ShowControl4J Roadmap

This is the living source of truth for where the project is going and why. Every
significant architecture or process decision made on this project should be recorded
in the **Decision Log** below at the time it's made, not reconstructed later. If
you're picking up work here (human or agent), read this file first.

Companion visual version (same content, presentation-friendly):
https://claude.ai/code/artifact/40510017-2571-411d-82a5-2fbffe260f86

## Where the project stands

Last active in April 2021. Three Maven modules (`showcontrol4j-core`,
`showcontrol4j-element`, `showcontrol4j-trigger`), one trigger (keyboard), one
element (a Raspberry Pi GPIO pin), RabbitMQ as the message broker, Pi4J v1 for
GPIO. Architecture is sound; scope is a skeleton. This roadmap takes it from
skeleton to a demoable, publishable, actively maintained show control library.

## Decision Log

Dated, most recent first. Each entry: the decision, the reasoning, and status.

### 2026-08-23 — `showcontrol4j-element` migrated to Pi4J V2
`GeneralPurposeIOShowElement` rewritten against Pi4J V2 (`com.pi4j:pi4j-core:4.0.2` -
confirmed by real dependency resolution and `javap` against the actual jar, not
documentation summaries). **Breaking API change**, intentional: the constructor now
takes a shared Pi4J `Context` (build once per application via `Pi4J.newAutoContext()`,
not per element - the context owns platform/provider registration for the whole JVM)
and a plain `int bcmPin` instead of a `Pin`. `turnOn/turnOff/toggle/pulse` now catch
and log Pi4J's checked `IOException` internally rather than propagating it, matching
how the rest of `ShowElement` already handles broker I/O errors. Unit tests rewritten
against `pi4j-plugin-mock` (`MockDigitalOutputProvider`) instead of Pi4J V1's
`SimulatedGpioProvider`, which no longer exists. This library intentionally does NOT
depend on a concrete hardware provider (e.g. `pi4j-plugin-gpiod` for Pi 5's RP1 chip,
or `pi4j-plugin-raspberrypi`) - that's an application-level choice, added by whoever
assembles the actual runnable show element, not baked into this library. **Status:
settled - see verification notes and open follow-ups below.**

**Verification:** `mvn clean install` (main + test compile) succeeded against the real,
network-resolved Pi4J V2 dependency tree - not just written to compile, actually
compiled. `showcontrol4j-core`'s full test suite (14/14) passed. The
`showcontrol4j-element` test suite could **not** be executed - see the JDK 24+
`system-rules` blocker below, which is pre-existing and unrelated to this change (it
fails identically on the untouched `ShowElementTest`). No physical Raspberry Pi
hardware was available to verify against real GPIO.

**Three prerequisite fixes, discovered while validating this change, needed to happen
first - all pre-existing issues, not introduced by this change:**
- **Lombok bumped 1.18.20 → 1.18.42** in `showcontrol4j-core` and `showcontrol4j-element`.
  1.18.20 cannot run at all on JDK 21+ (hard `javac` internals crash). This is the same
  fix the "Stay on Java" decision below already anticipated for the Java 25 bump - it
  turned out to be needed immediately, not just at that future step.
- **`maven-compiler-plugin` bumped 3.5.1 → 3.13.0**, with Lombok added to
  `annotationProcessorPaths` explicitly, in both modules. Without the explicit path,
  a clean build silently failed to run Lombok's annotation processing at all (missing
  `log` fields, missing `@Builder`/`@Data`-generated methods) - implicit classpath-based
  processor discovery does not reliably work with this plugin/JDK combination.
- **Confirmed Pi4J V2 4.0.2 itself requires a JDK 25 compiler/runtime to even read its
  class files** (class file version 69). This sandbox only had JDK 21 installed;
  `openjdk-25-jdk-headless` had to be installed to validate this change at all. This is
  hard confirmation that the "Java target: 25" decision below is a real requirement for
  this Pi4J version, not just a nice-to-have.

**New blocker found, not fixed here - needed before/during `bump-java-25`:**
`com.github.stefanbirkner:system-rules` (`ExpectedSystemExit`, used to test
`ShowElement`'s `System.exit(0)` shutdown path) calls `System.setSecurityManager()`,
which unconditionally throws on JDK 24+ (`SecurityManager` was permanently removed,
JEP 486). This breaks **every** test in `showcontrol4j-element` and (untested here but
almost certainly) `showcontrol4j-trigger`, not just Pi4J-related ones - the `@Rule` runs
before every test method regardless of what that test does. `bump-java-25` cannot be
called done until this is resolved - likely by replacing `system-rules` or refactoring
`ShowElement.runShutdown()`'s `System.exit()` call to be tested some other way.

### 2026-08-23 — Java target: 25 (LTS), not 17/21
Bumping straight to Java 25 rather than 17/21. It's a legitimate current LTS
(GA September 16, 2025 — an actual Long-Term-Support release on the
17 → 21 → 25 cadence, not a short-lived interim build), and targeting the
newest LTS now buys the longest runway before the next mandatory bump.
Lombok has supported JDK 25 since v1.18.42 (released alongside JDK 25's GA)
— the dependency-bump task must pin to that version or newer, not just
"latest." Structured concurrency (JEP 505) is still in preview in JDK 25
(5th preview, API still changing release to release) — don't build core
library code on it; scoped values (JEP 506) finalized in 25 and are fine to
use if useful. Two follow-on effects outside the version bump itself: (1)
Raspberry Pi OS's apt-installable OpenJDK packages lag badly behind current
releases — setup docs must point contributors at Eclipse Temurin (Adoptium)
JDK 25 aarch64 builds, not `apt install default-jdk`; (2) publishing the
library compiled for JDK 25 means anyone consuming it needs JDK 25+ too, a
real if small accessibility cost against the "encourage entry" goal — worth
stating plainly in the quickstart rather than a silent surprise. **Status:
settled.**

### 2026-08-22 — Governance & contribution process established
Added this roadmap file, a PR template, and `CLAUDE.md` for agentic/AI-assisted
contributors. Branch naming convention: `jhare/short-description-of-task`, no
ticket/task/epic numbers in branch names. Branches are squashed to a single commit
before merge. **Status: settled.**

### 2026-08-22 — Stay on Java for the core library; add a Python reference client later
Java isn't the bottleneck people assume — the "bloated" reputation comes from
enterprise app-server contexts, not a lean library on Pi-class hardware (1-8GB RAM,
no meaningful GC jitter for cue-based effects, JVM startup irrelevant for a
long-running daemon). True microcontroller nodes (ESP32/Arduino) can't run a JVM
regardless of what language the core is written in — that's a hardware constraint,
not a language choice — so they'll always run their own C++/MicroPython firmware.
MQTT is the interoperability layer, not shared code. To keep the project accessible
to non-Java contributors (a stated goal), the wire protocol (MQTT topics + JSON
message schema) will be documented as a standalone spec, and an official Python
reference client will ship as a companion package. **Status: settled.**

### 2026-08-22 — Messaging: migrate RabbitMQ → MQTT (Mosquitto)
MQTT is the protocol the target audience (haunt builders, prop makers,
ESP32/Arduino hobbyists) already uses. Mosquitto runs on the same Pi as the show,
no separate broker box needed. Retained messages let a rebooted element recover
show state instantly; Last-Will-and-Testament lets the broker detect an element
dropping off the network. Scoped to `showcontrol4j-core` only — the trigger/element
abstractions don't change, just the transport underneath them. Broker must be
secured with auth + TLS from the start, even on a local network. **Status: settled,
scheduled for Phase 1.**

### 2026-08-22 — Deployment stays local-first, always
No live cue-triggering path may depend on internet connectivity — matches how
professional show control (QLab, Medialon, Alcorn McBride) operates. Cloud's role
is scoped to non-critical-path concerns only: project website, an optional
after-the-fact show-log dashboard, firmware distribution. **Status: settled.**

### 2026-08-22 — Domain, site hosting, and blog
Domain repurchased through Cloudflare Registrar (at-cost, single vendor instead of
a separate registrar pointed at Cloudflare DNS). Site hosted on Cloudflare Pages,
connected directly to the site's GitHub repo (Pages builds and deploys on every
push to main — no separate CI needed for this). Blog content is low-volume, so it's
written as markdown files built with Hugo (single static binary, no npm dependency
chain, first-class Cloudflare Pages framework preset) rather than a CMS. Javadocs
are not self-hosted — javadoc.io generates and hosts them automatically once a
release lands on Maven Central. Site lives in its own repo, separate from the Java
library monorepo, to keep release tags clean of content commits. **Status:
settled.**

### 2026-08-22 — Publish to Maven Central via the new Central Portal
The old OSSRH JIRA-based process this project's `pom.xml` currently points to
(`s01.oss.sonatype.org`) is being retired by Sonatype in favor of the Central
Portal (central.sonatype.com). Namespace verification for `org.showcontrol4j` is
done via a DNS TXT record, which is why the domain needs to be repurchased before
this step. Verify the exact current process against central.sonatype.com when
Phase 4 starts — Sonatype's migration has been a moving target. **Status: settled,
scheduled for Phase 4.**

<!--
  Add new entries above this line, newest first. Format:
  ### YYYY-MM-DD — Short decision title
  What was decided, why, and current status (settled / needs revisit / superseded).
-->

## Phases

### Phase 0 — Groundwork
Lock decisions so nothing gets rebuilt twice.
- [x] Messaging protocol decision confirmed (MQTT)
- [ ] Domain repurchased via Cloudflare Registrar
- [x] Repo governance docs added (this file, PR template, `CLAUDE.md`)

### Phase 1 — Toolchain Modernization
Get the existing three modules onto a foundation that isn't already obsolete.
- [x] Pi4J v1 → v2 migration (v1's WiringPi base is dead, doesn't run on current
      Pi hardware — mandatory, not polish) — compiled and core-module-verified; see
      Decision Log 2026-08-23 for what is and isn't confirmed
- [ ] RabbitMQ client → MQTT client (Paho or HiveMQ) in `showcontrol4j-core`
- [ ] Mosquitto secured with auth + TLS from day one
- [ ] Fail-safe watchdog: an element defaults to idle/off if it loses the broker
      connection, instead of freezing mid-state
- [ ] Java 11 → 25 LTS (Eclipse Temurin aarch64 builds on Pi — see Decision Log)
- [ ] Pin `maven-compiler-plugin` to a version supporting `--release 25`
- [ ] Travis CI → GitHub Actions (build + test on push/PR)
- [ ] Dependency bump: Jackson, Lombok (≥1.18.42 for JDK 25 support), SLF4J, Mockito

### Phase 2 — Core Library Expansion
Go from "one trigger, one element" to a library that covers a themed attraction.
- [ ] Cue/timeline abstraction — biggest functional gap: real show control needs
      sequenced, timed cues, not just a single GO broadcast
- [ ] Triggers: IR break-beam, PIR motion, RFID/NFC
- [ ] Elements: servo, relay/solenoid, DMX512 output, audio playback
- [ ] ESP32/Arduino-compatible client (opens up cheap effect nodes)
- [ ] MQTT topic + JSON message schema documented as a standalone protocol spec
- [ ] Official reference Python client (paho-mqtt)

### Phase 3 — Reference Build
One physical, working demo proving the whole chain end to end.
- [ ] Short physical track/mock-up with one break-beam trigger
- [ ] 2-3 synced effects across separate Pis/ESP32s triggered off one pass
- [ ] Recovery demo: kill an effect's power mid-show, show it rejoin cleanly

### Phase 4 — Publish & Launch
Make the project findable, installable, and citable.
- [ ] Maven Central Portal setup, first `2.0.0` release
- [ ] Cloudflare Pages site live on the reclaimed domain
- [ ] README + docs rewritten around the new architecture
- [ ] First 2-3 YouTube videos: origin story, toolchain rebuild, reference build
- [ ] Discord reactivated, linked from README and the new site
- [ ] `CONTRIBUTING.md`, issue templates, a few `good-first-issue` tasks
- [ ] 15-minute quickstart tutorial, separate from Javadoc
- [ ] Semver discipline + `CHANGELOG.md` starting at `2.0.0`
- [ ] Trademark/name check on "ShowControl4J" before the publicity push

### Phase 5 — Grow
Sustain a content and contribution cadence instead of a one-time launch.
- [ ] Community-requested triggers/elements
- [ ] A bigger installation — multiple trigger points, a real cue sequence
- [ ] Regular build-log content, not just launch content

## Watch-outs

- **Pi4J v1 is dead.** Built on WiringPi, deprecated by its own author, doesn't
  reliably run on current Raspberry Pi hardware. Migrate to v2 in Phase 1, before
  more GPIO code is written against v1.
- **Sonatype's OSSRH → Central Portal migration** may have shifted since this was
  written — check central.sonatype.com directly at Phase 4 rather than following
  stale instructions.
- **RabbitMQ → MQTT touches only `showcontrol4j-core`.** The trigger/element
  abstractions don't need to change, just the transport underneath them.
- **`system-rules`' `ExpectedSystemExit` is incompatible with JDK 24+** (relies on
  `SecurityManager`, permanently removed in JEP 486). Breaks every test in a class that
  uses it, not just the one testing `System.exit()`. Must be resolved before
  `bump-java-25` can be called done — see Decision Log 2026-08-23.
- **JDK 25 isn't in Raspberry Pi OS's apt repos.** Setup docs must direct
  contributors to Eclipse Temurin (Adoptium) aarch64 builds — `apt install
  default-jdk` will not give you JDK 25.
