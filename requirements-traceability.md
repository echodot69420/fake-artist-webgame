# Requirements Traceability Matrix

Scope: every requirement marked **Implemented** in `requirements.md` (FR-01 – FR-15, NFR-01, NFR-05, NFR-06, NFR-07).
Tests are listed as `TestClass.testMethod`; use your editor's symbol search to jump to one.
Each test also carries `@Tag` annotations for its requirement IDs (sub-requirement and parent, e.g. `FR-01-01` and `FR-01`), or `NON-REQ`.
Run one requirement's tests with `mvn verify -Dgroups=FR-14`.


## Functional requirements

| Req | Summary | Tests | Coverage |
| --- | --- | --- | --- |
| FR-01 | Host a game | (via children) | Covered |
| FR-01-01 | Validate host input | `GameApiErrorIT.usernameLength0IsRejected`, `GameApiErrorIT.usernameLength1IsAccepted`, `GameApiErrorIT.usernameLength16IsAccepted`, `GameApiErrorIT.usernameLength17IsRejected`, `GameApiErrorIT.usernameWithInvalidCharacterIsRejected`, `GameApiErrorIT.missingUsernameIsRejected`, `GameApiErrorIT.themeLength0IsRejected`, `GameApiErrorIT.whitespaceOnlyThemeIsRejected`, `GameApiErrorIT.themeLength1IsAccepted`, `GameApiErrorIT.themeLength20IsAccepted`, `GameApiErrorIT.themeLength21IsRejected`, `GameControllerTest.hostRejectsEmptyTheme`, `GameControllerTest.hostRejectsUsernameWithLeadingWhitespace` | Covered |
| FR-01-02 | Unique 4-char code | `GameApiIT.hostAndConnectFlowCreatesPlayersForGame` (length only) | Partial – uniqueness not tested |
| FR-01-03 | New game in lobby state | `GameControllerTest.hostCreatesGameWithTrimmedValuesAndHostPlayer`, `GameApiIT.hostEndpointCreatesGameForValidInput` | Covered |
| FR-01-04 | Requester is host | `GameControllerTest.hostCreatesGameWithTrimmedValuesAndHostPlayer` | Covered |
| FR-01-05 | Return game representation | `GameApiIT.hostAndConnectFlowCreatesPlayersForGame`, `GameApiIT.hostEndpointCreatesGameForValidInput` | Covered |
| FR-02 | Join a game | (via children) | Covered |
| FR-02-01 | Normalize/validate code | `GameApiErrorIT.codeLength3IsRejected`, `GameApiErrorIT.codeLength4IsAccepted`, `GameApiErrorIT.codeLength5IsRejected`, `GameApiErrorIT.lowercaseCodeIsAccepted`, `GameControllerTest.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | Covered |
| FR-02-02 | 404 for missing game | `GameApiErrorIT.unknownCodeIsNotFound`, `GameControllerTest.connectRejectsUnknownGame` | Covered |
| FR-02-03 | 409 for started game | `GameApiErrorIT.joiningAStartedGameIsRejected` | Partial – topic-collection state not tested |
| FR-02-04 | 409 for full game | `GameApiErrorIT.tenthPlayerCanJoin`, `GameApiErrorIT.eleventhPlayerIsRejected` | Covered |
| FR-02-05 | Duplicate username (case-insensitive) | `GameApiErrorIT.duplicateUsernameIgnoringCaseIsRejected` | Covered |
| FR-02-06 | Add joining player | `GameApiIT.hostAndConnectFlowCreatesPlayersForGame` | Covered |
| FR-02-07 | Return updated game | `GameApiIT.hostAndConnectFlowCreatesPlayersForGame` | Covered |
| FR-03 | Username validity & uniqueness | `GameApiErrorIT.usernameLength0IsRejected`, `GameApiErrorIT.usernameLength1IsAccepted`, `GameApiErrorIT.usernameLength16IsAccepted`, `GameApiErrorIT.usernameLength17IsRejected`, `GameApiErrorIT.usernameWithInvalidCharacterIsRejected`, `GameApiErrorIT.missingUsernameIsRejected`, `GameApiErrorIT.duplicateUsernameIgnoringCaseIsRejected`, `GameControllerTest.hostRejectsUsernameWithLeadingWhitespace` | Covered |
| FR-04 | Theme validity | `GameApiErrorIT.themeLength0IsRejected`, `GameApiErrorIT.whitespaceOnlyThemeIsRejected`, `GameApiErrorIT.themeLength1IsAccepted`, `GameApiErrorIT.themeLength20IsAccepted`, `GameApiErrorIT.themeLength21IsRejected`, `GameControllerTest.hostRejectsEmptyTheme` | Covered |
| FR-05 | 10-player limit | `GameApiErrorIT.tenthPlayerCanJoin`, `GameApiErrorIT.eleventhPlayerIsRejected` | Covered |
| FR-06 | Lobby displays state | `LobbyPageIT.lobbyShowsCodeThemePlayersAndHost` | Covered (browser) |
| FR-07 | Real-time lobby updates | (via children) | Partial |
| FR-07-01 | One WS per player, code + id required | `LobbyHandlerConnectionTest.unknownGameConnectionReceivesActionableBye` | Partial |
| FR-07-02 | Validate WS game/player | `LobbyHandlerConnectionTest.unknownGameConnectionReceivesActionableBye`, `LobbyHandlerConnectionTest.unknownPlayerConnectionDoesNotMutateGameState` | Covered |
| FR-07-03 | Broadcast player-list changes | `GameControllerTest.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | Partial – join only; leave/timeout/remove not tested |
| FR-07-04 | Broadcast host change | — | **No test** (`GameControllerTest.removeHostTransfersHostRoleToNextPlayer` checks state, not broadcast) |
| FR-07-05 | Broadcast topic progress | — | **No test** |
| FR-07-06 | Broadcast started state | — | **No test** |
| FR-08 | Host-only start | (via children) | Partial |
| FR-08-01 | Non-host start rejected | `LobbyHandlerStartTest.nonHostCannotStart` | Covered |
| FR-08-02 | ≥3 players to start | `LobbyHandlerStartTest.startWithTwoPlayersIsRejected`, `LobbyHandlerStartTest.startWithThreePlayersIsAccepted` | Covered |
| FR-08-03 | Open topic collection | `LobbyHandlerStartTest.startWithThreePlayersIsAccepted` | Partial – topic prompt to all clients not asserted |
| FR-08-04 | No duplicate start | — | **No test** |
| FR-09 | Collect one topic per player | (via children) | Partial |
| FR-09-01 | Topics only during phase | `LobbyHandlerTopicTest.topicBeforeStartIsRejected` | Covered |
| FR-09-02 | Validate topic (1–40, trimmed) | `LobbyHandlerTopicTest.emptyTopicIsRejected`, `LobbyHandlerTopicTest.whitespaceOnlyTopicIsRejected`, `LobbyHandlerTopicTest.oneCharacterTopicIsAccepted`, `LobbyHandlerTopicTest.fortyCharacterTopicIsAccepted`, `LobbyHandlerTopicTest.fortyOneCharacterTopicIsRejected` | Covered |
| FR-09-03 | One topic per player | — | **No test** |
| FR-09-04 | Broadcast topic progress | — | **No test** |
| FR-09-05 | Complete collection when all submit | `LobbyHandlerFeatureFlagTest.disabledRoundResultsFlagDoesNotSendExperimentalMessage` (incidental) | Partial |
| FR-10 | Assign roles | (via children) | Partial |
| FR-10-01 | Topic from non-fake player | — | **No test** |
| FR-10-02 | Exactly one fake artist | — | **No test** |
| FR-10-03 | Private role/topic message | `LobbyHandlerFeatureFlagTest.disabledRoundResultsFlagDoesNotSendExperimentalMessage` (incidental) | Partial |
| FR-10-04 | Mark started | — | **No test** |
| FR-10-05 | Close join/topic after start | `GameApiErrorIT.joiningAStartedGameIsRejected` (join) | Partial – topic rejection after start not tested |
| FR-11 | Safe departures | (via children) | Partial |
| FR-11-01 | Explicit quit | — | **No test** |
| FR-11-02 | WS close removes player | — | **No test** |
| FR-11-03 | Heartbeat timeout removal | — | **No test** |
| FR-11-04 | Host transfer | `GameControllerTest.removeHostTransfersHostRoleToNextPlayer` | Covered |
| FR-11-05 | Update topic collection on leave | — | **No test** |
| FR-11-06 | Delete empty game | `GameControllerTest.removingLastPlayerDeletesGame` | Covered |
| FR-11-07 | Broadcast post-departure state | — | **No test** |
| FR-12 | WS liveness limits | (via children) | **No test** |
| FR-12-01 – FR-12-05 | Heartbeat, last-seen, away, 15 s / 120 s timeouts | — | **No test** |
| FR-13 | Copy game code | `LobbyPageIT.copyButtonCopiesCodeAndConfirms` | Covered (browser) |
| FR-14 | Actionable error feedback | `GameApiErrorIT.codeLength3IsRejected`, `GameApiErrorIT.codeLength4IsAccepted`, `GameApiErrorIT.codeLength5IsRejected`, `GameApiErrorIT.lowercaseCodeIsAccepted`, `GameApiErrorIT.unknownCodeIsNotFound`, `GameApiErrorIT.tenthPlayerCanJoin`, `GameApiErrorIT.eleventhPlayerIsRejected`, `GameApiErrorIT.duplicateUsernameIgnoringCaseIsRejected`, `GameApiErrorIT.joiningAStartedGameIsRejected`, `GameApiErrorIT.usernameLength0IsRejected`, `GameApiErrorIT.usernameLength1IsAccepted`, `GameApiErrorIT.usernameLength16IsAccepted`, `GameApiErrorIT.usernameLength17IsRejected`, `GameApiErrorIT.usernameWithInvalidCharacterIsRejected`, `GameApiErrorIT.missingUsernameIsRejected`, `GameApiErrorIT.themeLength0IsRejected`, `GameApiErrorIT.whitespaceOnlyThemeIsRejected`, `GameApiErrorIT.themeLength1IsAccepted`, `GameApiErrorIT.themeLength20IsAccepted`, `GameApiErrorIT.themeLength21IsRejected`, `GameApiErrorIT.malformedJsonIsRejected`, `GameApiErrorIT.emptyBodyIsRejected`, `GameApiErrorIT.nonJsonContentTypeIsRejected`, `LobbyHandlerStartTest.startWithTwoPlayersIsRejected`, `LobbyHandlerStartTest.startWithThreePlayersIsAccepted`, `LobbyHandlerStartTest.nonHostCannotStart`, `LobbyHandlerTopicTest.emptyTopicIsRejected`, `LobbyHandlerTopicTest.whitespaceOnlyTopicIsRejected`, `LobbyHandlerTopicTest.oneCharacterTopicIsAccepted`, `LobbyHandlerTopicTest.fortyCharacterTopicIsAccepted`, `LobbyHandlerTopicTest.fortyOneCharacterTopicIsRejected`, `LobbyHandlerTopicTest.topicBeforeStartIsRejected`, `LobbyHandlerProtocolTest.malformedJsonIsRejectedAndSessionStaysOpen`, `LobbyHandlerProtocolTest.unknownTypeIsRejected`, `LobbyHandlerProtocolTest.missingTypeIsRejected`, `WebSocketErrorHandlerTest.successfulActionSendsNoErrorFrame`, `WebSocketErrorHandlerTest.apiExceptionBecomesErrorFrameWithDefaultMessage`, `WebSocketErrorHandlerTest.apiExceptionCustomMessageIsUsedAsDetail`, `WebSocketErrorHandlerTest.everyCodeIsSentByName`, `ErrorCodeTest.everyCodeHasANonBlankDefaultMessage`; `HomePageIT` (all error tests) and `LobbyPageIT.webSocketErrorShowsBanner` check the messages are shown in the UI; `HomePageIT.unreachableServerShowsConnectionMessage` covers REST connection failure | Covered |
| FR-15 | `/health` and `/models` | `GameApiIT.healthEndpointReturnsApplicationStatus` | Partial – `/models` not tested |

## Non-functional requirements

| Req | Summary | Tests | Coverage |
| --- | --- | --- | --- |
| NFR-01 | Server-side input validation | `GameApiErrorIT.malformedJsonIsRejected`, `GameApiErrorIT.emptyBodyIsRejected`, `GameApiErrorIT.nonJsonContentTypeIsRejected`, `LobbyHandlerProtocolTest.unknownTypeIsRejected`, `LobbyHandlerProtocolTest.missingTypeIsRejected`, `LobbyHandlerConnectionTest.unknownPlayerConnectionDoesNotMutateGameState` (plus all FR-01-01/FR-09-02 validation tests) | Covered |
| NFR-05 | Reproducible (README) | — | Doc review only |
| NFR-06 | CI runs `mvn test` on 17/21 | — | Verified by the GitHub Actions workflow, not a test |
| NFR-07 | Stable during disconnection | `WebSocketErrorHandlerTest.checkedExceptionBecomesInternalAndDoesNotPropagate`, `WebSocketErrorHandlerTest.closedSessionIsNotSentTo`, `WebSocketErrorHandlerTest.sendFailureIsSwallowed`, `WebSocketErrorHandlerTest.errorsNeverCloseTheSession`, `LobbyHandlerProtocolTest.malformedJsonIsRejectedAndSessionStaysOpen`, `LobbyHandlerProtocolTest.connectionSurvivesAnError` | Partial – error paths only; quit/close/timeout paths untested |

## Test index

| Test | Requirement(s) |
| --- | --- |
| `FeatureFlagsIT.roundResultsIsOffByDefault` | no requirement |
| `FeatureFlagsIT.kickPlayerIsOffByDefault` | FR-16, planned |
| `GameApiIT.healthEndpointReturnsApplicationStatus` | FR-15 |
| `GameApiIT.hostAndConnectFlowCreatesPlayersForGame` | FR-01-02, FR-01-05, FR-02-06, FR-02-07 |
| `GameApiIT.hostEndpointCreatesGameForValidInput` | FR-01-03, FR-01-05 |
| `HostEndpointPerformanceIT.hostEndpointRespondsWithinPerformanceThreshold` | NFR-11, partial |
| `LobbyHandlerConnectionTest.unknownGameConnectionReceivesActionableBye` | FR-07-01, FR-07-02 |
| `LobbyHandlerConnectionTest.unknownPlayerConnectionDoesNotMutateGameState` | FR-07-02, NFR-01 |
| `ErrorCodeTest.everyCodeHasANonBlankDefaultMessage` | FR-14 |
| `ErrorCodeTest.everyCodeMapsToAnErrorStatus` | NFR-14 |
| `ErrorCodeTest.onlyServerFailuresAre5xx` | NFR-14 |
| `LobbyHandlerFeatureFlagTest.enabledRoundResultsFlagSendsExperimentalMessage` | no requirement |
| `FeatureFlagsIT.WhenOverridden.propertiesTurnFlagsOn` | NFR-13, partial |
| `LobbyHandlerFeatureFlagTest.disabledRoundResultsFlagDoesNotSendExperimentalMessage` | no requirement; touches FR-09-05, FR-10-03 |
| `GameControllerTest.hostCreatesGameWithTrimmedValuesAndHostPlayer` | FR-01-03, FR-01-04 |
| `GameControllerTest.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | FR-02-01, FR-07-03 |
| `GameControllerTest.removeHostTransfersHostRoleToNextPlayer` | FR-11-04 |
| `GameControllerTest.removingLastPlayerDeletesGame` | FR-11-06 |
| `GameControllerTest.connectRejectsUnknownGame` | FR-02-02 |
| `GameControllerTest.removePlayerRejectsUnknownPlayer` | no requirement |
| `GameControllerTest.hostRejectsEmptyTheme` | FR-01-01, FR-04 |
| `GameControllerTest.hostRejectsUsernameWithLeadingWhitespace` | FR-01-01, FR-03 |
| `LobbyHandlerKickTest.disabledKickFlagRejectsKickWithoutChangingGame` | FR-16, planned |
| `LobbyHandlerKickTest.nonHostCannotKickAnotherPlayer` | FR-16-02, planned |
| `LobbyHandlerKickTest.hostCannotKickUnknownPlayer` | FR-16-03, planned |
| `LobbyHandlerKickTest.hostKickRemovesTargetAndNotifiesBothSessions` | FR-16-04, FR-16-05, planned |
| `LobbyHandlerKickTest.hostCannotKickThemself` | FR-16-03 |
| `LobbyHandlerProtocolTest.malformedJsonIsRejectedAndSessionStaysOpen` | FR-14, NFR-07 |
| `LobbyHandlerProtocolTest.unknownTypeIsRejected` | FR-14, NFR-01 |
| `LobbyHandlerProtocolTest.missingTypeIsRejected` | FR-14, NFR-01 |
| `LobbyHandlerProtocolTest.connectionSurvivesAnError` | NFR-07 |
| `LobbyHandlerStartTest.startWithTwoPlayersIsRejected` | FR-08-02, FR-14 |
| `LobbyHandlerStartTest.startWithThreePlayersIsAccepted` | FR-08-02, FR-08-03, FR-14 |
| `LobbyHandlerStartTest.nonHostCannotStart` | FR-08-01, FR-14 |
| `LobbyHandlerTopicTest.emptyTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerTopicTest.whitespaceOnlyTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerTopicTest.oneCharacterTopicIsAccepted` | FR-09-02, FR-14 |
| `LobbyHandlerTopicTest.fortyCharacterTopicIsAccepted` | FR-09-02, FR-14 |
| `LobbyHandlerTopicTest.fortyOneCharacterTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerTopicTest.topicBeforeStartIsRejected` | FR-09-01, FR-14 |
| `GameApiErrorIT.codeLength3IsRejected` | FR-02-01, FR-14 |
| `GameApiErrorIT.codeLength4IsAccepted` | FR-02-01, FR-14 |
| `GameApiErrorIT.codeLength5IsRejected` | FR-02-01, FR-14 |
| `GameApiErrorIT.lowercaseCodeIsAccepted` | FR-02-01, FR-14 |
| `GameApiErrorIT.unknownCodeIsNotFound` | FR-02-02, FR-14 |
| `GameApiErrorIT.validationLogNamesFieldButNotValue` | NFR-12 |
| `GameApiErrorIT.expectedErrorIsLoggedWithoutStackTrace` | NFR-12 |
| `RestErrorHandlerTest.unexpectedExceptionIsLoggedAtErrorWithStackTrace` | NFR-12 |
| `GameApiErrorIT.malformedJsonIsRejected` | FR-14, NFR-01 |
| `GameApiErrorIT.emptyBodyIsRejected` | FR-14, NFR-01 |
| `GameApiErrorIT.nonJsonContentTypeIsRejected` | FR-14, NFR-01 |
| `GameApiErrorIT.missingStaticResourceIsQuiet404` | NFR-12 |
| `RestErrorHandlerTest.unexpectedExceptionReturnsInternalWithoutLeakingMessage` | NFR-12 |
| `GameApiErrorIT.tenthPlayerCanJoin` | FR-02-04, FR-05, FR-14 |
| `GameApiErrorIT.eleventhPlayerIsRejected` | FR-02-04, FR-05, FR-14 |
| `GameApiErrorIT.duplicateUsernameIgnoringCaseIsRejected` | FR-02-05, FR-03, FR-14 |
| `GameApiErrorIT.joiningAStartedGameIsRejected` | FR-02-03, FR-10-05, FR-14 |
| `GameApiErrorIT.themeLength0IsRejected` | FR-01-01, FR-04, FR-14 |
| `GameApiErrorIT.whitespaceOnlyThemeIsRejected` | FR-01-01, FR-04, FR-14 |
| `GameApiErrorIT.themeLength1IsAccepted` | FR-01-01, FR-04, FR-14 |
| `GameApiErrorIT.themeLength20IsAccepted` | FR-01-01, FR-04, FR-14 |
| `GameApiErrorIT.themeLength21IsRejected` | FR-01-01, FR-04, FR-14 |
| `GameApiErrorIT.usernameLength0IsRejected` | FR-01-01, FR-03, FR-14 |
| `GameApiErrorIT.usernameLength1IsAccepted` | FR-01-01, FR-03, FR-14 |
| `GameApiErrorIT.usernameLength16IsAccepted` | FR-01-01, FR-03, FR-14 |
| `GameApiErrorIT.usernameLength17IsRejected` | FR-01-01, FR-03, FR-14 |
| `GameApiErrorIT.usernameWithInvalidCharacterIsRejected` | FR-01-01, FR-03, FR-14 |
| `GameApiErrorIT.missingUsernameIsRejected` | FR-01-01, FR-03, FR-14 |
| `WebSocketErrorHandlerTest.successfulActionSendsNoErrorFrame` | FR-14 |
| `WebSocketErrorHandlerTest.apiExceptionBecomesErrorFrameWithDefaultMessage` | FR-14 |
| `WebSocketErrorHandlerTest.apiExceptionCustomMessageIsUsedAsDetail` | FR-14 |
| `WebSocketErrorHandlerTest.unexpectedRuntimeExceptionBecomesInternalWithoutLeakingMessage` | NFR-12 |
| `WebSocketErrorHandlerTest.checkedExceptionBecomesInternalAndDoesNotPropagate` | NFR-07 |
| `WebSocketErrorHandlerTest.closedSessionIsNotSentTo` | NFR-07 |
| `WebSocketErrorHandlerTest.sendFailureIsSwallowed` | NFR-07 |
| `WebSocketErrorHandlerTest.errorsNeverCloseTheSession` | NFR-07 |
| `WebSocketErrorHandlerTest.everyCodeIsSentByName` | FR-14, NFR-14 |
| `HomePageIT.takenUsernameShowsErrorUnderUsername` | FR-02-05, FR-03, FR-14 |
| `HomePageIT.unknownGameShowsErrorUnderCode` | FR-02-02, FR-14 |
| `HomePageIT.fullGameShowsServerMessage` | FR-02-04, FR-05, FR-14 |
| `HomePageIT.startedGameShowsServerMessage` | FR-02-03, FR-14 |
| `HomePageIT.serverThemeErrorShowsUnderTheme` | FR-14 (stubbed response) |
| `HomePageIT.unreachableServerShowsConnectionMessage` | FR-14 (stubbed network failure) |
| `HomePageIT.malformedErrorBodyShowsFallbackMessage` | FR-14, NFR-04 (stubbed response) |
| `HomePageIT.shortCodeIsRejectedWithoutRequest` | FR-02-01 |
| `HomePageIT.codeInputIsUppercasedAndFiltered` | FR-02-01 |
| `HomePageIT.hostingOpensLobbyForNewGame` | FR-01-05 |
| `LobbyPageIT.lobbyShowsCodeThemePlayersAndHost` | FR-06 |
| `LobbyPageIT.secondPlayerAppearsWithoutReload` | FR-07-03 |
| `LobbyPageIT.copyButtonCopiesCodeAndConfirms` | FR-13 |
| `LobbyPageIT.webSocketErrorShowsBanner` | FR-08-01, FR-14 |

## Tests not traced to a completed requirement

| Test | Notes |
| --- | --- |
| `FeatureFlagsIT.roundResultsIsOffByDefault`, `LobbyHandlerFeatureFlagTest.enabledRoundResultsFlagSendsExperimentalMessage`, `LobbyHandlerFeatureFlagTest.disabledRoundResultsFlagDoesNotSendExperimentalMessage` | Round-results feature flag – no requirement exists (`LobbyHandlerFeatureFlagTest.disabledRoundResultsFlagDoesNotSendExperimentalMessage` incidentally touches FR-09-05/FR-10-03) |
| `FeatureFlagsIT.kickPlayerIsOffByDefault`, `LobbyHandlerKickTest.disabledKickFlagRejectsKickWithoutChangingGame`, `LobbyHandlerKickTest.nonHostCannotKickAnotherPlayer`, `LobbyHandlerKickTest.hostCannotKickUnknownPlayer`, `LobbyHandlerKickTest.hostKickRemovesTargetAndNotifiesBothSessions`, `LobbyHandlerKickTest.hostCannotKickThemself` | FR-16 kick – requirement still **Planned** |
| `HostEndpointPerformanceIT.hostEndpointRespondsWithinPerformanceThreshold` | NFR-11 – only **Partially implemented** |
| `GameApiErrorIT.missingStaticResourceIsQuiet404`, `RestErrorHandlerTest.unexpectedExceptionReturnsInternalWithoutLeakingMessage`, `GameApiErrorIT.validationLogNamesFieldButNotValue`, `GameApiErrorIT.expectedErrorIsLoggedWithoutStackTrace`, `RestErrorHandlerTest.unexpectedExceptionIsLoggedAtErrorWithStackTrace`, `WebSocketErrorHandlerTest.unexpectedRuntimeExceptionBecomesInternalWithoutLeakingMessage` | NFR-12 logging – only **Partially implemented** |
| `ErrorCodeTest.everyCodeMapsToAnErrorStatus`, `ErrorCodeTest.onlyServerFailuresAre5xx` | NFR-14 – only **Partially implemented** (also support FR-14) |
| `GameControllerTest.removePlayerRejectsUnknownPlayer` | No matching requirement (closest: NFR-02, partial) |
