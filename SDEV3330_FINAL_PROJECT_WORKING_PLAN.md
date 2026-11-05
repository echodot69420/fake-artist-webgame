# SDEV 3330 Final Project Working Plan

This document turns the existing **Fake Artist Goes to New York** project into a
more complete set of requirements and project-management artifacts for the SDEV
3330 final submission. It is based on the current repository, issue history,
commit history, tests, and implementation as of November 5, 2026.

This is a working document, not a substitute for the student's personal
reflection. Sections marked **Personal reflection needed** must be completed
with explanations of decisions and experiences that the author can defend.

## Current project baseline

- **Repository:** `https://github.com/echodot69420/fake-artist-webgame`
- **Application:** A browser-based multiplayer party drawing-game lobby.
- **Backend:** Java 17, Spring Boot 3.5.6, REST endpoints, and WebSocket
  communication.
- **Frontend:** `index.html` for hosting/joining and `lobby.html` for the
  real-time lobby.
- **Current limits:** 3–10 players, usernames of 1–16 alphanumeric characters,
  themes of 1–20 characters, and four-character game codes.
- **State model:** In-memory games and players; restarting the server clears
  active games.
- **Testing:** JUnit/Spring tests cover game creation, joining, validation,
  duplicate names, player limits, host transfer, game deletion, health checks,
  and a basic performance threshold.
- **Automation:** GitHub Actions runs `mvn test` on pushes.

## Work already completed

The existing work should be credited and connected to the final report:

| Evidence | What it demonstrates |
|---|---|
| Initial repository and FastAPI “Hello World” commits | Project setup and early prototype |
| `eeb57fa` — switched to Java | Technology/process change and implementation divergence |
| Username boundary and punctuation commits | Requirements refinement through testing |
| WebSocket/lobby implementation | Real-time multiplayer architecture |
| `734d590` — endpoint and performance tests | Test-driven requirement verification |
| `00ceeaf` — CI workflow | Automated validation |
| `adad595` — visual adjustments and start-game fix | Iterative UX work late in the project |
| Issues 1–18 | Project tasks and historical workflow |
| Project status-change events | Evidence that the project board changed over time |

## Consolidated requirements

### Functional requirements

| ID | Requirement | Acceptance criteria | Status |
|---|---|---|---|
| FR-01 | A user can host a game with a username and theme. | A valid request creates a unique four-character code, stores the host as the first player, and returns the game data. | Implemented |
| FR-02 | A user can join an existing game with a username and code. | A valid code adds a non-host player and returns the updated player list. Codes are case-insensitive. | Implemented |
| FR-03 | Usernames must be unique within a game. | Joining with a name already used in that game returns HTTP 409 and a clear error. | Implemented |
| FR-04 | Games support between 3 and 10 players. | The server prevents an eleventh player from joining and prevents the host from starting with fewer than three players. | Implemented |
| FR-05 | The lobby displays current game and player information. | Connected clients receive the game code, theme, player list, host status, and started status. | Implemented |
| FR-06 | Lobby clients maintain a persistent connection. | The browser connects to `/ws/lobby`, sends heartbeats, and receives player-list updates. | Implemented |
| FR-07 | The host can start the game from the lobby. | A host with at least three players can send the start action; non-hosts and undersized games receive an error. | Implemented |
| FR-08 | Players can leave a game. | A quit action removes the player, transfers host status when necessary, and deletes an empty game. | Implemented |
| FR-09 | Inactive connections are removed. | Active tabs time out after the documented heartbeat interval; hidden tabs receive the longer away timeout. | Implemented |
| FR-10 | The application exposes a health endpoint. | `GET /health` returns HTTP 200 and reports service, game count, and player count. | Implemented |
| FR-11 | The application provides basic item CRUD endpoints. | The documented item endpoints create, list, retrieve, and delete items with appropriate success and not-found responses. | Legacy/demo feature; document whether retained |
| FR-12 | Users receive understandable validation and connection errors. | Invalid usernames, themes, codes, missing games, full games, and unauthorized start attempts produce visible or API-readable errors. | Partially implemented; verify UI coverage |

### Non-functional requirements

| ID | Requirement | Acceptance criteria | Status |
|---|---|---|---|
| NFR-01 | The application must be testable and repeatable. | `mvn test` completes successfully in CI. | Implemented |
| NFR-02 | Host creation should respond quickly under normal local-test conditions. | The existing performance test verifies 20 valid host requests under 500 ms each in the test environment. | Implemented; explain that this is not a production SLA |
| NFR-03 | The application must validate input at the server boundary. | Server-side validation rejects malformed usernames, themes, and codes instead of relying only on browser validation. | Implemented |
| NFR-04 | The repository must be reproducible for a reviewer. | README documents Java/Maven prerequisites, build, test, launch, routes, and WebSocket behavior. | Mostly implemented |
| NFR-05 | Configuration must not require committed secrets. | No secrets are committed; port and environment-specific values are documented and overridable. | Needs explicit documentation |
| NFR-06 | The repository must preserve a meaningful development history. | Incremental commits explain setup, technology changes, tests, fixes, CI, and UX changes. | Implemented |
| NFR-07 | The interface should be usable on common screen sizes. | Homepage and lobby remain readable and usable at desktop and narrow viewport widths; keyboard focus is visible. | Needs manual verification evidence |
| NFR-08 | The system should handle normal disconnects gracefully. | Closing a tab, quitting, and heartbeat timeout remove a player and update remaining clients without crashing the server. | Implemented in code; add targeted tests/evidence |

## Known requirement changes and implementation decisions

These are suitable for the requirements-divergence section of the report:

1. The project began as a FastAPI/Python prototype and was later converted to
   Java/Spring Boot. Explain why the change was made, what work had to be
   repeated, and what was gained or lost.
2. Username requirements were refined through tests. The final implementation
   accepts one to sixteen alphanumeric characters and rejects punctuation and
   leading whitespace.
3. Game state is in memory rather than persistent. This is appropriate for a
   course-sized prototype but means games disappear when the server restarts.
4. The repository still contains the original item CRUD demo endpoints. Decide
   whether they are part of the final product, explicitly legacy, or candidates
   for removal.
5. The current implementation is a lobby and start-flow foundation rather than
   a complete drawing round. The report should state the actual scope instead of
   implying features that are not implemented.

## Improved task backlog

Create or update GitHub Issues using the following titles, descriptions, and
acceptance criteria. Keep completed historical issues for traceability, but use
these more precise tasks for remaining work.

### Epic: Requirements and documentation

| Priority | Task | Acceptance criteria |
|---|---|---|
| P0 | Consolidate functional and non-functional requirements | This document or a linked requirements document contains every requirement, acceptance criteria, status, and known divergence. |
| P0 | Document the risk register and issue log | At least five initial risks, their probability/impact, mitigations, actual outcomes, and one newly discovered risk are recorded. |
| P0 | Write the final reflection report | The report contains all nine sections in order, is 4–6 pages, and includes the repository link at the top. |
| P0 | Add architecture diagram and caption | The report embeds a diagram showing browser pages, REST controller, WebSocket handler, in-memory state, and CI. |
| P1 | Add board screenshots and captions | At least one screenshot shows the board during active work and explains what was happening at that point. |

### Epic: GitHub project management

| Priority | Task | Acceptance criteria |
|---|---|---|
| P0 | Rewrite remaining work as user stories | Each active story uses “As a [role], I want [goal], so that [benefit]” and includes testable acceptance criteria. |
| P0 | Create epic labels | Use consistent labels such as `epic:requirements`, `epic:lobby`, `epic:quality`, and `epic:documentation`. |
| P0 | Create meaningful milestones | Add milestones aligned to project checkpoints, for example `MVP Lobby`, `Real-Time Stability`, `Quality and CI`, and `Final Submission`. |
| P1 | Link historical issues to epics and milestones | Existing issues are labeled and assigned where appropriate; incomplete work is visible in the final milestone. |
| P1 | Capture board history | Save screenshots or issue-event evidence showing backlog, active work, and completed work at multiple points. |

### Epic: Product completion and quality

| Priority | Task | Acceptance criteria |
|---|---|---|
| P1 | Add WebSocket disconnect integration tests | Tests cover quit, close, timeout, host transfer, and broadcast behavior. |
| P1 | Verify responsive and keyboard-accessible UI | Test at narrow and wide viewport sizes, document results, and fix any blocking issues. |
| P1 | Clarify or remove legacy item endpoints | README and final requirements identify their purpose; unrelated demo behavior does not confuse the product scope. |
| P1 | Document configuration management | Explain port overrides, local versus production assumptions, and secret handling. |
| P2 | Add a Maven Wrapper | A reviewer can run the build without manually installing a matching Maven version. |
| P2 | Clean tracked build output | Remove any already-tracked generated files under `target/` and verify `.gitignore` prevents recurrence. |

## Recommended user stories

These replace vague task titles such as “Create Host Page” with outcome-focused
stories:

- **As a player, I want to host a lobby with a name and theme, so that other
  players can join my game.**
- **As a player, I want to join with a short game code, so that I can enter the
  correct lobby quickly.**
- **As a player, I want to see connected players update in real time, so that I
  know who is ready.**
- **As the host, I want to start only when at least three players are present,
  so that the game has enough participants.**
- **As a player, I want duplicate or invalid names rejected clearly, so that I
  can correct my input without guessing.**
- **As a host, I want host responsibilities transferred if I leave, so that the
  lobby remains usable.**
- **As a maintainer, I want automated tests to run on every push, so that
  regressions are detected before submission.**
- **As a reviewer, I want documented setup and configuration steps, so that I
  can run the application without private knowledge.**

## Risk register starter

Complete the outcome column from actual experience rather than guessing.

| Risk | Probability | Impact | Planned mitigation | Actual outcome / response |
|---|---:|---:|---|---|
| Technology migration from Python to Java takes longer than expected | High | High | Freeze API behavior, migrate in small steps, preserve tests | **Personal reflection needed** |
| WebSocket disconnects leave stale players | Medium | High | Heartbeats, away mode, scheduled sweeper, disconnect cleanup | **Personal reflection needed** |
| Requirements change after implementation | Medium | Medium | Convert boundary decisions into tests and update requirements | **Personal reflection needed** |
| A late UI defect blocks the demo | Medium | High | Test the host/join/start flow before each milestone | The missing start-game button was fixed in the final visual-adjustment commit; explain the response |
| CI or dependency setup fails for a reviewer | Medium | Medium | Document prerequisites, commit build metadata, run CI on pushes | **Personal reflection needed** |
| In-memory state is lost during restart | Medium | Medium | State limitation is documented; persistence is out of current scope | **Personal reflection needed** |
| Unexpected risk discovered during the semester | — | — | Add the risk that was not visible during initial planning | **Personal reflection needed** |

## Architecture and design notes

The current system can be described as a small monolithic Spring Boot
application with lightweight layers:

1. Browser pages provide the user interface.
2. `GameController` handles HTTP routes and in-memory game state.
3. `LobbyHandler` handles WebSocket connections, messages, heartbeats, and
   broadcasts.
4. `Game` and record types represent the domain data.
5. GitHub Actions provides automated test execution.

Useful design concepts to discuss are separation of HTTP and WebSocket
responsibilities, immutable record types for request/player data, centralized
validation/cleanup helpers, and the tradeoff of keeping the prototype in one
Java file. **Personal reflection needed:** explain why this architecture fit the
project size and identify one principle deliberately followed or violated.

## Code-review self-assessment task

Review commit `adad595` as if it were a teammate's pull request. Discuss:

- whether the visual changes preserve the host/join flow;
- whether the start-game fix is covered by automated tests;
- whether the change should have been split into separate UI and behavior
  commits;
- whether any accessibility or responsive behavior needs verification;
- whether the commit message gives enough context;
- whether AI assistance was used and how the code was checked.

End with a clear approval decision and explain what would be required before
approval if changes are needed.

## Final report checklist

- [ ] Repository link is the first line of the report.
- [ ] Sections 1–9 appear in the required order.
- [ ] SDLC model is compared with at least one alternative.
- [ ] One-person Agile/Scrum practice is described honestly.
- [ ] Requirements and acceptance criteria match the actual implementation.
- [ ] User stories, epics, milestones, and board history are evidenced.
- [ ] Risk register outcomes are based on actual events.
- [ ] Architecture diagram is embedded near Section 5.
- [ ] Git history, Maven dependencies, lock/build reproducibility, and
      configuration management are explained.
- [ ] A meaningful commit or pull request is reviewed.
- [ ] One real-world parallel or failure mode is analyzed.
- [ ] One biggest change and one transferable skill are identified.
- [ ] The report is 4–6 pages and is written in the student's own voice.
