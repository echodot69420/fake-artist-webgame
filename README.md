# SDEV-3310 — Fake Artist Goes to New York

A party drawing game for 3–10 players, built with **Spring Boot** and vanilla HTML/CSS/JS.
One player gets the same secret theme as everyone else — except the fake artist,
who is faking it.

## Setup

**Prerequisites:** Java 17+ and Maven. Check with `java --version` and `mvn --version`.

To run the automated checks for the game logic and API endpoints:

```bash
mvn test
```

1. **Get the code** (skip if you already have it):

   ```bash
   git clone <repo-url>
   cd SDEV-Project
   ```

2. **Build the application:**

   ```bash
   mvn clean package
   ```

   This downloads the Spring Boot and WebSocket dependencies through Maven.

## Launch

```bash
java -jar target/fake-artist-1.0.0.jar
```

You should see:

```
Started FakeArtistApplication in ... seconds
```

Then open <http://127.0.0.1:8000> in a browser. Restart the application after
code changes.

**Stop the server** with `Ctrl+C` in the terminal.

### Verify it's running

```bash
curl http://127.0.0.1:8000/health   # should return {"status":"ok",...}
curl http://127.0.0.1:8000/items     # should return []
```

Or open the interactive API docs at <http://127.0.0.1:8000/docs>.

### Play a quick round

1. On one machine, open <http://127.0.0.1:8000>, enter a **username** and a
   **theme** (1–20 characters), and click **Host a game**. Note the 4-letter code.
2. On another machine or browser window, open the same address, enter a
   **username** and the **code**, then click **Join**.
3. Both players land in the lobby at `/lobby` with a live player list.
   Everyone stays connected via the WebSocket described below.

> To let players on *other* devices connect, run the server on
> `0.0.0.0` instead of `127.0.0.1` and have them visit
> `http://<your-local-IP>:8000`. The homepage builds its WebSocket URL from
> the page host, so no other changes are needed.

### Troubleshooting

| Problem | Fix |
| ------- | --- |
| `mvn` is not recognized | Install Maven and ensure it is on `PATH`. |
| `Address already in use` (port 8000) | Another process has the port: stop it, or configure a different Spring Boot port. |
| Page loads but the lobby says "Connecting…" | Make sure you're opening `http://127.0.0.1:8000/lobby` through the server, not `lobby.html` as a local file — the WebSocket URL is derived from the page host. |
| Players disappear after ~15 s | Expected: the server drops silent connections. Check the browser console for WebSocket errors. |
| "Game not found" when joining | The server restarted, which wipes all games (state is in memory). Host a new one.
| Console fills with `GET /health … 404` every 30 s | Local tooling (e.g. OpenCode) probing port 8000, which is also vLLM's default. `GET /health` now answers `200` and those lines are filtered out of the access log. |

## Pages

| Route     | File          | Purpose                                             |
| --------- | ------------- | --------------------------------------------------- |
| `/`       | `index.html`  | Homepage: username, theme, host/join form           |
| `/lobby`  | `lobby.html`  | Lobby: game code, theme, live player list           |

The homepage stores the session (`code`, `player_id`, `username`, `theme`) in
`sessionStorage` and redirects to `/lobby`, which opens the WebSocket below.

## HTTP API

| Method | Path            | Body                                    | Returns                     |
| ------ | --------------- | --------------------------------------- | --------------------------- |
| POST   | `/host`         | `{username, theme}`                     | New `Game` (creator is host) |
| POST   | `/connect`      | `{username, code}`                      | Updated `Game`              |
| POST   | `/quit`         | `{code, player_id}`                     | Remaining players, or deletion |
| GET    | `/game/{code}`  | —                                       | Current game state          |
| GET    | `/health`       | —                                       | `{"status": "ok", ...}`     |
| GET    | `/models`       | —                                       | Empty OpenAI-style model list |

- **Username:** 2–16 characters, unique per game.
- **Theme:** 1–20 characters, chosen by the host.
- **Code:** 4 characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`
  (no `I`, `O`, `0`, `1`), case-insensitive on connect.

Errors: `404` game/player not found · `409` game full, already started, or
username taken · `422` invalid username, theme, or code.

### Health and quiet logging

`GET /health` returns `200 {"status": "ok", "service", "games", "players"}` —
useful for `curl` checks and for local tooling that probes port 8000 (that
port is also vLLM's default, so tools like OpenCode poll `/health` every
30 seconds; once it answers they follow up with `GET /models`, which returns
an empty list so nothing gets registered).

Those probe requests are answered normally by Spring Boot, so nothing needs to
be registered in the model picker.

### Demo endpoints

The item CRUD demo from the original simple server is still there:
`GET /items`, `GET /items/{id}`, `POST /items`, `DELETE /items/{id}`.

## WebSocket: `/ws/lobby`

One persistent connection per player, opened by `lobby.html`:

```
ws://127.0.0.1:8000/ws/lobby?code=ABCD&player_id=...
```

| Direction  | Message                                       |
| ---------- | --------------------------------------------- |
| client → server | `{"type": "ping"}` — heartbeat            |
| client → server | `{"type": "away"}` — tab hidden, relaxed timeout |
| client → server | `{"type": "quit"}` — leave the game      |
| server → client | `{"type": "players", code, theme, players, started, game_exists}"` |
| server → client | `{"type": "pong"}` — heartbeat reply     |
| server → client | `{"type": "bye", "reason", "game_exists"}` — being disconnected |

### Liveness checks

The client pings every **5 seconds**. A background sweeper runs every
**15 seconds** and disconnects anyone silent for longer than their timeout:

| State          | Timeout |
| -------------- | ------- |
| Active tab     | 15 s    |
| Hidden tab (`away`) | 120 s (browsers throttle hidden-tab timers) |

On timeout the server sends a `bye`, closes the socket, removes the player
from the game (host duties pass to the next player), and broadcasts the new
player list. Closing the tab or losing the network triggers the same cleanup.

## Project layout

```
pom.xml         Maven build and Spring Boot dependencies
src/main/java/  Spring Boot app: static pages, lobby HTTP API, WebSocket
index.html      Homepage (host/join form with theme field)
lobby.html      Lobby page (live connection + player list)
```

All state is in memory — games reset when the server restarts.
