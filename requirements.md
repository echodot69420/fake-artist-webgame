See [requirements-traceability.md](requirements-traceability.md) for the requirement-to-test traceability matrix.

## 1. Functional requirements

| ID | Requirement | Verification criteria | Status |
| --- | --- | --- | --- |
| FR-01 | The system shall allow a player to host a game. | The host flow shall satisfy FR-01-01 through FR-01-05. | Implemented |
| FR-01-01 | The system shall validate host input before creating a game. | Given invalid username or theme input, the system shall reject the request and shall not create a game. | Implemented |
| FR-01-02 | The system shall generate a unique game code. | Each active game shall have a distinct four-character code. | Implemented |
| FR-01-03 | The system shall create a new game in lobby state. | A newly created game shall contain the selected theme, an empty topic collection, and the requesting player. | Implemented |
| FR-01-04 | The system shall assign the requesting player as host. | The first player in a newly created game shall have host status. | Implemented |
| FR-01-05 | The system shall return the created game representation. | A successful host request shall return the game code, theme, player list, and started state. | Implemented |
| FR-02 | The system shall allow a player to join an existing game. | The join flow shall satisfy FR-02-01 through FR-02-07. | Implemented |
| FR-02-01 | The system shall normalize and validate a submitted game code. | The system shall trim and case-normalize the code and shall reject a code that is not four alphanumeric characters. | Implemented |
| FR-02-02 | The system shall reject a join request for a nonexistent game. | The system shall return HTTP 404 and shall not modify game state when no matching game exists. | Implemented |
| FR-02-03 | The system shall reject a join request for a started or topic-collection game. | The system shall return HTTP 409 and shall not add the player when joining is closed. | Implemented |
| FR-02-04 | The system shall reject a join request for a full game. | The system shall return HTTP 409 when the game contains 10 players. | Implemented |
| FR-02-05 | The system shall reject a duplicate username. | Username comparison shall be case-insensitive, and the system shall return HTTP 409 without adding the player. | Implemented |
| FR-02-06 | The system shall add a valid joining player. | The system shall assign a unique player identifier and shall preserve the existing host assignment. | Implemented |
| FR-02-07 | The system shall return the updated game representation. | A successful join request shall return the game code, theme, and complete current player list. | Implemented |
| FR-03 | The system shall enforce username validity and uniqueness. | The system shall accept only usernames matching `[A-Za-z0-9]{1,16}`. The system shall reject a username already used in the game with HTTP 409 and an explanatory error. | Implemented |
| FR-04 | The system shall enforce theme validity. | The system shall reject a null, empty, or more-than-20-character theme with HTTP 422 and an explanatory error. | Implemented |
| FR-05 | The system shall enforce the player limit. | The system shall permit no more than 10 players in a game and shall reject an eleventh player with HTTP 409. | Implemented |
| FR-06 | The lobby shall display current game state. | The lobby shall display the game code, theme, connected players, host indicator, and started state received from the server. | Implemented |
| FR-07 | The system shall distribute lobby state updates in real time. | The real-time lobby flow shall satisfy FR-07-01 through FR-07-06. | Implemented |
| FR-07-01 | Each player shall establish one WebSocket connection to `/ws/lobby`. | A valid game code and player identifier shall be required to establish the connection. | Implemented |
| FR-07-02 | The server shall validate the WebSocket game and player association. | The server shall close a connection when the game does not exist or the player is not a member of the game. | Implemented |
| FR-07-03 | The server shall broadcast player-list changes. | Connected clients shall receive updates after a player joins, leaves, times out, or is removed. | Implemented |
| FR-07-04 | The server shall broadcast host-status changes. | Connected clients shall receive the updated host assignment after the host leaves. | Implemented |
| FR-07-05 | The server shall broadcast topic-collection progress. | Connected clients shall receive submitted and total player counts during topic collection. | Implemented |
| FR-07-06 | The server shall broadcast the started state. | Connected clients shall receive an update when topic collection completes. | Implemented |
| FR-08 | The system shall permit only an eligible host to start round setup. | The start flow shall satisfy FR-08-01 through FR-08-04. | Implemented |
| FR-08-01 | The server shall authorize the start action by current host status. | A start action from a non-host shall be rejected and shall not change game state. | Implemented |
| FR-08-02 | The server shall require at least three players before starting. | A start action with fewer than three players shall be rejected with an error. | Implemented |
| FR-08-03 | The server shall open topic collection after a valid start action. | The game shall enter topic collection and all connected clients shall receive a topic prompt. | Implemented |
| FR-08-04 | The server shall prevent duplicate start actions. | A game already collecting topics or started shall not open another topic-collection phase. | Implemented |
| FR-09 | The system shall collect one topic from each player before starting the round. | The topic-collection flow shall satisfy FR-09-01 through FR-09-05. | Implemented |
| FR-09-01 | The server shall accept topics only during the active topic-collection phase. | A topic submitted outside the active phase shall be rejected. | Implemented |
| FR-09-02 | The server shall validate each submitted topic. | The server shall accept only trimmed, non-empty topics of 1-40 characters. | Implemented |
| FR-09-03 | The server shall accept no more than one topic per player per phase. | A player who has submitted shall not submit a replacement during the same phase. | Implemented |
| FR-09-04 | The server shall broadcast topic-submission progress. | Each connected client shall receive submitted-topic and current-player counts. | Implemented |
| FR-09-05 | The server shall complete topic collection when all current players have submitted. | The system shall stop topic collection, assign roles, and start the game after all current players have submitted valid topics. | Implemented |
| FR-10 | The system shall assign round roles after topic collection completes. | The role-assignment flow shall satisfy FR-10-01 through FR-10-05. | Implemented |
| FR-10-01 | The system shall select one submitted topic for the round. | The selected topic shall be from a non-fake-artist player's submission. | Implemented |
| FR-10-02 | The system shall select exactly one fake artist. | Exactly one current player shall be assigned the fake-artist role. | Implemented |
| FR-10-03 | The system shall send each player a private role or topic message. | The fake artist shall receive a fake-artist message; every other player shall receive the selected topic. | Implemented |
| FR-10-04 | The system shall mark the game as started after role assignment. | The game representation shall report `started` as true after role notifications are issued. | Implemented |
| FR-10-05 | The system shall close joining and topic submission after role assignment. | The server shall reject later join requests and topic submissions for the started game. | Implemented |
| FR-11 | The system shall process player departures safely. | The departure flow shall satisfy FR-11-01 through FR-11-07. | Implemented |
| FR-11-01 | The system shall process an explicit quit request. | A valid quit request shall remove the identified player and notify remaining clients. | Implemented |
| FR-11-02 | The system shall process a closed WebSocket connection. | Closing a player's WebSocket shall remove that player if the player is still present. | Implemented |
| FR-11-03 | The system shall process a heartbeat timeout. | A player exceeding the applicable liveness limit shall be removed and receive a close notification when possible. | Implemented |
| FR-11-04 | The system shall transfer host status when the host leaves. | The first remaining player shall become host when the current host is removed. | Implemented |
| FR-11-05 | The system shall update topic collection after a player leaves. | A removed player's pending topic shall be removed, and topic progress shall be recalculated for remaining players. | Implemented |
| FR-11-06 | The system shall delete an empty game. | When the last player leaves, the system shall remove the game and invalidate its code. | Implemented |
| FR-11-07 | The system shall broadcast the resulting game state. | Remaining connected clients shall receive the updated player list and game state after departure processing. | Implemented |
| FR-12 | The system shall enforce WebSocket connection liveness limits. | The liveness flow shall satisfy FR-12-01 through FR-12-05. | Implemented |
| FR-12-01 | The client shall send a heartbeat every five seconds while connected. | The client shall send a `ping` message at the documented interval. | Implemented |
| FR-12-02 | The server shall record the latest activity for each connection. | A valid heartbeat or supported client message shall update the connection's last-seen time. | Implemented |
| FR-12-03 | The server shall support an away state for hidden tabs. | An away client shall send an `away` message and shall receive the extended timeout. | Implemented |
| FR-12-04 | The server shall remove a silent active connection after more than 15 seconds. | The scheduled liveness check shall remove an active connection that exceeds the active timeout. | Implemented |
| FR-12-05 | The server shall remove a silent away connection after more than 120 seconds. | The scheduled liveness check shall remove an away connection that exceeds the away timeout. | Implemented |
| FR-13 | The lobby shall provide game-code sharing. | The lobby shall provide a copy-to-clipboard control and shall provide visible confirmation after a successful copy operation. | Implemented |
| FR-14 | The system shall provide actionable error feedback. | The system shall provide a visible UI message or API error for an invalid code, missing game, full game, started game, unauthorized start, invalid topic, invalid input, or connection failure. | Implemented |
| FR-15 | The service shall provide operational endpoints. | `GET /health` shall return HTTP 200 with status, service name, game count, and player count. `GET /models` shall return an empty model list for local tooling compatibility. | Implemented |
| FR-16 | The system shall allow the host to remove a player from the lobby. | The kick flow shall satisfy FR-16-01 through FR-16-05. | Planned |
| FR-16-01 | The lobby shall display kick controls only to the current host. | A non-host client shall not receive an enabled kick control. | Planned |
| FR-16-02 | The server shall authorize each kick action. | A kick request from a non-host shall be rejected without changing game state. | Planned |
| FR-16-03 | The server shall reject invalid kick targets. | The server shall reject requests targeting the host, the requesting player, or an unknown player. | Planned |
| FR-16-04 | The server shall remove a valid kick target. | The server shall remove the target and disconnect the target's WebSocket with a clear reason. | Planned |
| FR-16-05 | The server shall broadcast the post-kick game state. | Remaining connected clients shall receive the updated player list after a successful kick. | Planned |

## 2. Non-functional requirements

| ID | Requirement | Verification criteria | Status |
| --- | --- | --- | --- |
| NFR-01 | The server shall enforce all input validation rules at the service boundary. | HTTP and WebSocket inputs shall be validated by the server independently of browser-side validation; invalid input shall receive a documented error response and shall not mutate valid game state. | Implemented |
| NFR-02 | The server shall enforce game and player authorization. | HTTP and WebSocket operations shall verify the requested game and player association; a player shall not access or modify another game, and an unauthorized action shall be rejected without state mutation. | Partially implemented; kick authorization remains planned |
| NFR-03 | The application shall protect temporary game data from unrelated players. | Usernames, topics, player identifiers, and game state shall be returned only to clients associated with the relevant game; the application shall not persist this data beyond the in-memory game lifecycle. | Partially implemented; privacy tests shall be added |
| NFR-04 | The application shall provide safe error responses. | Error responses shall identify the corrective action without exposing stack traces, credentials, internal paths, or unrelated game data. | Partially implemented; response coverage shall be verified |
| NFR-05 | The project shall be reproducible by a reviewer. | `README.md` shall document Java 17 or later, Maven, build, test, launch, HTTP routes, WebSocket messages, configuration overrides, and the in-memory state limitation. | Implemented |
| NFR-06 | The project shall execute automated quality checks in continuous integration. | GitHub Actions shall run `mvn verify` on Java 17 and Java 21 and shall fail the workflow when compilation or tests fail. | Implemented |
| NFR-07 | The server shall remain stable during normal player disconnection. | Quit, close, duplicate cleanup, and heartbeat-timeout paths shall preserve valid game state, notify remaining players, and shall not terminate the application. | Implemented in code; integration-test coverage shall be expanded |
| NFR-08 | The user interface shall conform to basic keyboard accessibility requirements. | All host, join, lobby, topic, copy, quit, and planned kick controls shall be keyboard operable; form controls shall have programmatically associated labels; focus shall remain visibly identifiable. | Partially implemented; manual accessibility verification remains |
| NFR-09 | The user interface shall communicate status and errors accessibly. | Validation errors, connection errors, topic progress, copy confirmation, and game-state changes shall be presented through readable text and appropriate live-region semantics without relying only on color. | Partially implemented; accessibility review remains |
| NFR-10 | The application shall support the project's target browser and viewport range. | The host, join, lobby, and topic flows shall remain operable in current Chrome, Firefox, and Edge releases at viewport widths from 320 px through 1440 px. | Partially implemented; compatibility verification remains |
| NFR-11 | The host endpoint shall meet the prototype responsiveness target. | In the documented test environment, at least 95% of 20 concurrent valid host requests shall complete within 500 ms, and the benchmark shall not be treated as a production service-level agreement. | Partially implemented; the existing benchmark shall be aligned with this criterion |
| NFR-12 | The application shall provide operational observability. | The server shall log startup failures, validation failures, game lifecycle events, WebSocket cleanup events, and unexpected errors without logging credentials, player identifiers, or complete submitted topics. | Partially implemented; logging requirements shall be verified |
| NFR-13 | The project shall separate environment configuration from source code. | Port, feature flags, and environment-specific settings shall be overrideable through documented configuration or command-line properties; secrets shall not be committed. | Partially implemented; configuration documentation shall be expanded |
| NFR-14 | The project shall maintain testable and maintainable code. | Each changed behavior shall have an automated test where practical; public HTTP and WebSocket contracts shall be documented; generated build output shall remain untracked. | Partially implemented; disconnect and kick tests remain |
