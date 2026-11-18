# Requirements Traceability Matrix

Scope: every requirement marked **Implemented** in `requirements.md` (FR-01 – FR-15, NFR-01, NFR-05, NFR-06, NFR-07).
Tests are listed as `TestClass.testMethod`; use your editor's symbol search to jump to one.


## Functional requirements

| Req | Summary | Tests | Coverage |
| --- | --- | --- | --- |
| FR-01 | Host a game | (via children) | Covered |
| FR-01-01 | Validate host input | `RestErrorHandlerTests.usernameLength0IsRejected`, `RestErrorHandlerTests.usernameLength1IsAccepted`, `RestErrorHandlerTests.usernameLength16IsAccepted`, `RestErrorHandlerTests.usernameLength17IsRejected`, `RestErrorHandlerTests.usernameWithInvalidCharacterIsRejected`, `RestErrorHandlerTests.missingUsernameIsRejected`, `RestErrorHandlerTests.themeLength0IsRejected`, `RestErrorHandlerTests.whitespaceOnlyThemeIsRejected`, `RestErrorHandlerTests.themeLength1IsAccepted`, `RestErrorHandlerTests.themeLength20IsAccepted`, `RestErrorHandlerTests.themeLength21IsRejected`, `GameControllerUnitTests.hostRejectsEmptyTheme`, `GameControllerUnitTests.hostRejectsUsernameWithLeadingWhitespace` | Covered |
| FR-01-02 | Unique 4-char code | `FakeArtistApplicationTests.hostAndConnectFlowCreatesPlayersForGame` (length only) | Partial – uniqueness not tested |
| FR-01-03 | New game in lobby state | `GameControllerUnitTests.hostCreatesGameWithTrimmedValuesAndHostPlayer`, `FakeArtistApplicationTests.hostEndpointCreatesGameForValidInput` | Covered |
| FR-01-04 | Requester is host | `GameControllerUnitTests.hostCreatesGameWithTrimmedValuesAndHostPlayer` | Covered |
| FR-01-05 | Return game representation | `FakeArtistApplicationTests.hostAndConnectFlowCreatesPlayersForGame`, `FakeArtistApplicationTests.hostEndpointCreatesGameForValidInput` | Covered |
| FR-02 | Join a game | (via children) | Covered |
| FR-02-01 | Normalize/validate code | `RestErrorHandlerTests.codeLength3IsRejected`, `RestErrorHandlerTests.codeLength4IsAccepted`, `RestErrorHandlerTests.codeLength5IsRejected`, `RestErrorHandlerTests.lowercaseCodeIsAccepted`, `GameControllerUnitTests.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | Covered |
| FR-02-02 | 404 for missing game | `RestErrorHandlerTests.unknownCodeIsNotFound`, `GameControllerUnitTests.connectRejectsUnknownGame` | Covered |
| FR-02-03 | 409 for started game | `RestErrorHandlerTests.joiningAStartedGameIsRejected` | Partial – topic-collection state not tested |
| FR-02-04 | 409 for full game | `RestErrorHandlerTests.tenthPlayerCanJoin`, `RestErrorHandlerTests.eleventhPlayerIsRejected` | Covered |
| FR-02-05 | Duplicate username (case-insensitive) | `RestErrorHandlerTests.duplicateUsernameIgnoringCaseIsRejected` | Covered |
| FR-02-06 | Add joining player | `FakeArtistApplicationTests.hostAndConnectFlowCreatesPlayersForGame` | Covered |
| FR-02-07 | Return updated game | `FakeArtistApplicationTests.hostAndConnectFlowCreatesPlayersForGame` | Covered |
| FR-03 | Username validity & uniqueness | `RestErrorHandlerTests.usernameLength0IsRejected`, `RestErrorHandlerTests.usernameLength1IsAccepted`, `RestErrorHandlerTests.usernameLength16IsAccepted`, `RestErrorHandlerTests.usernameLength17IsRejected`, `RestErrorHandlerTests.usernameWithInvalidCharacterIsRejected`, `RestErrorHandlerTests.missingUsernameIsRejected`, `RestErrorHandlerTests.duplicateUsernameIgnoringCaseIsRejected`, `GameControllerUnitTests.hostRejectsUsernameWithLeadingWhitespace` | Covered |
| FR-04 | Theme validity | `RestErrorHandlerTests.themeLength0IsRejected`, `RestErrorHandlerTests.whitespaceOnlyThemeIsRejected`, `RestErrorHandlerTests.themeLength1IsAccepted`, `RestErrorHandlerTests.themeLength20IsAccepted`, `RestErrorHandlerTests.themeLength21IsRejected`, `GameControllerUnitTests.hostRejectsEmptyTheme` | Covered |
| FR-05 | 10-player limit | `RestErrorHandlerTests.tenthPlayerCanJoin`, `RestErrorHandlerTests.eleventhPlayerIsRejected` | Covered |
| FR-06 | Lobby displays state | — | **No automated test** (UI) |
| FR-07 | Real-time lobby updates | (via children) | Partial |
| FR-07-01 | One WS per player, code + id required | `LobbyHandlerConnectionTests.unknownGameConnectionReceivesActionableBye` | Partial |
| FR-07-02 | Validate WS game/player | `LobbyHandlerConnectionTests.unknownGameConnectionReceivesActionableBye`, `LobbyHandlerConnectionTests.unknownPlayerConnectionDoesNotMutateGameState` | Covered |
| FR-07-03 | Broadcast player-list changes | `GameControllerUnitTests.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | Partial – join only; leave/timeout/remove not tested |
| FR-07-04 | Broadcast host change | — | **No test** (`GameControllerUnitTests.removeHostTransfersHostRoleToNextPlayer` checks state, not broadcast) |
| FR-07-05 | Broadcast topic progress | — | **No test** |
| FR-07-06 | Broadcast started state | — | **No test** |
| FR-08 | Host-only start | (via children) | Partial |
| FR-08-01 | Non-host start rejected | `LobbyHandlerErrorTests.nonHostCannotStart` | Covered |
| FR-08-02 | ≥3 players to start | `LobbyHandlerErrorTests.startWithTwoPlayersIsRejected`, `LobbyHandlerErrorTests.startWithThreePlayersIsAccepted` | Covered |
| FR-08-03 | Open topic collection | `LobbyHandlerErrorTests.startWithThreePlayersIsAccepted` | Partial – topic prompt to all clients not asserted |
| FR-08-04 | No duplicate start | — | **No test** |
| FR-09 | Collect one topic per player | (via children) | Partial |
| FR-09-01 | Topics only during phase | `LobbyHandlerErrorTests.topicBeforeStartIsRejected` | Covered |
| FR-09-02 | Validate topic (1–40, trimmed) | `LobbyHandlerErrorTests.emptyTopicIsRejected`, `LobbyHandlerErrorTests.whitespaceOnlyTopicIsRejected`, `LobbyHandlerErrorTests.oneCharacterTopicIsAccepted`, `LobbyHandlerErrorTests.fortyCharacterTopicIsAccepted`, `LobbyHandlerErrorTests.fortyOneCharacterTopicIsRejected` | Covered |
| FR-09-03 | One topic per player | — | **No test** |
| FR-09-04 | Broadcast topic progress | — | **No test** |
| FR-09-05 | Complete collection when all submit | `LobbyHandlerFeatureFlagTests.disabledRoundResultsFlagDoesNotSendExperimentalMessage` (incidental) | Partial |
| FR-10 | Assign roles | (via children) | Partial |
| FR-10-01 | Topic from non-fake player | — | **No test** |
| FR-10-02 | Exactly one fake artist | — | **No test** |
| FR-10-03 | Private role/topic message | `LobbyHandlerFeatureFlagTests.disabledRoundResultsFlagDoesNotSendExperimentalMessage` (incidental) | Partial |
| FR-10-04 | Mark started | — | **No test** |
| FR-10-05 | Close join/topic after start | `RestErrorHandlerTests.joiningAStartedGameIsRejected` (join) | Partial – topic rejection after start not tested |
| FR-11 | Safe departures | (via children) | Partial |
| FR-11-01 | Explicit quit | — | **No test** |
| FR-11-02 | WS close removes player | — | **No test** |
| FR-11-03 | Heartbeat timeout removal | — | **No test** |
| FR-11-04 | Host transfer | `GameControllerUnitTests.removeHostTransfersHostRoleToNextPlayer` | Covered |
| FR-11-05 | Update topic collection on leave | — | **No test** |
| FR-11-06 | Delete empty game | `GameControllerUnitTests.removingLastPlayerDeletesGame` | Covered |
| FR-11-07 | Broadcast post-departure state | — | **No test** |
| FR-12 | WS liveness limits | (via children) | **No test** |
| FR-12-01 – FR-12-05 | Heartbeat, last-seen, away, 15 s / 120 s timeouts | — | **No test** |
| FR-13 | Copy game code | — | **No automated test** (UI) |
| FR-14 | Actionable error feedback | `RestErrorHandlerTests.codeLength3IsRejected`, `RestErrorHandlerTests.codeLength4IsAccepted`, `RestErrorHandlerTests.codeLength5IsRejected`, `RestErrorHandlerTests.lowercaseCodeIsAccepted`, `RestErrorHandlerTests.unknownCodeIsNotFound`, `RestErrorHandlerTests.tenthPlayerCanJoin`, `RestErrorHandlerTests.eleventhPlayerIsRejected`, `RestErrorHandlerTests.duplicateUsernameIgnoringCaseIsRejected`, `RestErrorHandlerTests.joiningAStartedGameIsRejected`, `RestErrorHandlerTests.usernameLength0IsRejected`, `RestErrorHandlerTests.usernameLength1IsAccepted`, `RestErrorHandlerTests.usernameLength16IsAccepted`, `RestErrorHandlerTests.usernameLength17IsRejected`, `RestErrorHandlerTests.usernameWithInvalidCharacterIsRejected`, `RestErrorHandlerTests.missingUsernameIsRejected`, `RestErrorHandlerTests.themeLength0IsRejected`, `RestErrorHandlerTests.whitespaceOnlyThemeIsRejected`, `RestErrorHandlerTests.themeLength1IsAccepted`, `RestErrorHandlerTests.themeLength20IsAccepted`, `RestErrorHandlerTests.themeLength21IsRejected`, `RestErrorHandlerTests.malformedJsonIsRejected`, `RestErrorHandlerTests.emptyBodyIsRejected`, `RestErrorHandlerTests.nonJsonContentTypeIsRejected`, `LobbyHandlerErrorTests.startWithTwoPlayersIsRejected`, `LobbyHandlerErrorTests.startWithThreePlayersIsAccepted`, `LobbyHandlerErrorTests.nonHostCannotStart`, `LobbyHandlerErrorTests.emptyTopicIsRejected`, `LobbyHandlerErrorTests.whitespaceOnlyTopicIsRejected`, `LobbyHandlerErrorTests.oneCharacterTopicIsAccepted`, `LobbyHandlerErrorTests.fortyCharacterTopicIsAccepted`, `LobbyHandlerErrorTests.fortyOneCharacterTopicIsRejected`, `LobbyHandlerErrorTests.topicBeforeStartIsRejected`, `LobbyHandlerErrorTests.malformedJsonIsRejectedAndSessionStaysOpen`, `LobbyHandlerErrorTests.unknownTypeIsRejected`, `LobbyHandlerErrorTests.missingTypeIsRejected`, `WebSocketErrorHandlerTests.successfulActionSendsNoErrorFrame`, `WebSocketErrorHandlerTests.apiExceptionBecomesErrorFrameWithDefaultMessage`, `WebSocketErrorHandlerTests.apiExceptionCustomMessageIsUsedAsDetail`, `WebSocketErrorHandlerTests.everyCodeIsSentByName`, `ErrorCodeTests.everyCodeHasANonBlankDefaultMessage`; connection failure manual | Covered |
| FR-15 | `/health` and `/models` | `FakeArtistApplicationTests.healthEndpointReturnsApplicationStatus` | Partial – `/models` not tested |

## Non-functional requirements

| Req | Summary | Tests | Coverage |
| --- | --- | --- | --- |
| NFR-01 | Server-side input validation | `RestErrorHandlerTests.malformedJsonIsRejected`, `RestErrorHandlerTests.emptyBodyIsRejected`, `RestErrorHandlerTests.nonJsonContentTypeIsRejected`, `LobbyHandlerErrorTests.unknownTypeIsRejected`, `LobbyHandlerErrorTests.missingTypeIsRejected`, `LobbyHandlerConnectionTests.unknownPlayerConnectionDoesNotMutateGameState` (plus all FR-01-01/FR-09-02 validation tests) | Covered |
| NFR-05 | Reproducible (README) | — | Doc review only |
| NFR-06 | CI runs `mvn test` on 17/21 | — | Verified by the GitHub Actions workflow, not a test |
| NFR-07 | Stable during disconnection | `WebSocketErrorHandlerTests.checkedExceptionBecomesInternalAndDoesNotPropagate`, `WebSocketErrorHandlerTests.closedSessionIsNotSentTo`, `WebSocketErrorHandlerTests.sendFailureIsSwallowed`, `WebSocketErrorHandlerTests.errorsNeverCloseTheSession`, `LobbyHandlerErrorTests.malformedJsonIsRejectedAndSessionStaysOpen`, `LobbyHandlerErrorTests.connectionSurvivesAnError` | Partial – error paths only; quit/close/timeout paths untested |

## Test index

| Test | Requirement(s) |
| --- | --- |
| `FakeArtistApplicationTests.roundResultsFeatureIsDisabledByDefault` | no requirement |
| `FakeArtistApplicationTests.kickPlayerFeatureIsDisabledByDefault` | FR-16, planned |
| `FakeArtistApplicationTests.healthEndpointReturnsApplicationStatus` | FR-15 |
| `FakeArtistApplicationTests.hostAndConnectFlowCreatesPlayersForGame` | FR-01-02, FR-01-05, FR-02-06, FR-02-07 |
| `FakeArtistApplicationTests.hostEndpointCreatesGameForValidInput` | FR-01-03, FR-01-05 |
| `FakeArtistApplicationTests.hostEndpointRespondsWithinPerformanceThreshold` | NFR-11, partial |
| `LobbyHandlerConnectionTests.unknownGameConnectionReceivesActionableBye` | FR-07-01, FR-07-02 |
| `LobbyHandlerConnectionTests.unknownPlayerConnectionDoesNotMutateGameState` | FR-07-02, NFR-01 |
| `ErrorCodeTests.everyCodeHasANonBlankDefaultMessage` | FR-14 |
| `ErrorCodeTests.everyCodeMapsToAnErrorStatus` | NFR-14 |
| `ErrorCodeTests.onlyServerFailuresAre5xx` | NFR-14 |
| `LobbyHandlerFeatureFlagConfigurationTests.configurationOverrideEnablesRoundResultsMessage` | no requirement |
| `LobbyHandlerFeatureFlagTests.disabledRoundResultsFlagDoesNotSendExperimentalMessage` | no requirement; touches FR-09-05, FR-10-03 |
| `GameControllerUnitTests.hostCreatesGameWithTrimmedValuesAndHostPlayer` | FR-01-03, FR-01-04 |
| `GameControllerUnitTests.connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers` | FR-02-01, FR-07-03 |
| `GameControllerUnitTests.removeHostTransfersHostRoleToNextPlayer` | FR-11-04 |
| `GameControllerUnitTests.removingLastPlayerDeletesGame` | FR-11-06 |
| `GameControllerUnitTests.connectRejectsUnknownGame` | FR-02-02 |
| `GameControllerUnitTests.removePlayerRejectsUnknownPlayer` | no requirement |
| `GameControllerUnitTests.hostRejectsEmptyTheme` | FR-01-01, FR-04 |
| `GameControllerUnitTests.hostRejectsUsernameWithLeadingWhitespace` | FR-01-01, FR-03 |
| `LobbyHandlerKickTests.disabledKickFlagRejectsKickWithoutChangingGame` | FR-16, planned |
| `LobbyHandlerKickTests.nonHostCannotKickAnotherPlayer` | FR-16-02, planned |
| `LobbyHandlerKickTests.hostCannotKickUnknownOrHostTarget` | FR-16-03, planned |
| `LobbyHandlerKickTests.enabledHostKickRemovesTargetAndNotifiesBothSessions` | FR-16-04, FR-16-05, planned |
| `LobbyHandlerErrorTests.hostKickingThemselfIsRejected` | FR-16-03 |
| `LobbyHandlerErrorTests.malformedJsonIsRejectedAndSessionStaysOpen` | FR-14, NFR-07 |
| `LobbyHandlerErrorTests.unknownTypeIsRejected` | FR-14, NFR-01 |
| `LobbyHandlerErrorTests.missingTypeIsRejected` | FR-14, NFR-01 |
| `LobbyHandlerErrorTests.connectionSurvivesAnError` | NFR-07 |
| `LobbyHandlerErrorTests.startWithTwoPlayersIsRejected` | FR-08-02, FR-14 |
| `LobbyHandlerErrorTests.startWithThreePlayersIsAccepted` | FR-08-02, FR-08-03, FR-14 |
| `LobbyHandlerErrorTests.nonHostCannotStart` | FR-08-01, FR-14 |
| `LobbyHandlerErrorTests.emptyTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerErrorTests.whitespaceOnlyTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerErrorTests.oneCharacterTopicIsAccepted` | FR-09-02, FR-14 |
| `LobbyHandlerErrorTests.fortyCharacterTopicIsAccepted` | FR-09-02, FR-14 |
| `LobbyHandlerErrorTests.fortyOneCharacterTopicIsRejected` | FR-09-02, FR-14 |
| `LobbyHandlerErrorTests.topicBeforeStartIsRejected` | FR-09-01, FR-14 |
| `RestErrorHandlerTests.codeLength3IsRejected` | FR-02-01, FR-14 |
| `RestErrorHandlerTests.codeLength4IsAccepted` | FR-02-01, FR-14 |
| `RestErrorHandlerTests.codeLength5IsRejected` | FR-02-01, FR-14 |
| `RestErrorHandlerTests.lowercaseCodeIsAccepted` | FR-02-01, FR-14 |
| `RestErrorHandlerTests.unknownCodeIsNotFound` | FR-02-02, FR-14 |
| `RestErrorHandlerTests.validationLogNamesFieldButNotValue` | NFR-12 |
| `RestErrorHandlerTests.expectedErrorIsLoggedWithoutStackTrace` | NFR-12 |
| `RestErrorHandlerUnexpectedErrorTests.unexpectedExceptionIsLoggedAtErrorWithStackTrace` | NFR-12 |
| `RestErrorHandlerTests.malformedJsonIsRejected` | FR-14, NFR-01 |
| `RestErrorHandlerTests.emptyBodyIsRejected` | FR-14, NFR-01 |
| `RestErrorHandlerTests.nonJsonContentTypeIsRejected` | FR-14, NFR-01 |
| `RestErrorHandlerTests.missingStaticResourceIsQuiet404` | NFR-12 |
| `RestErrorHandlerUnexpectedErrorTests.unexpectedExceptionReturnsInternalWithoutLeakingMessage` | NFR-12 |
| `RestErrorHandlerTests.tenthPlayerCanJoin` | FR-02-04, FR-05, FR-14 |
| `RestErrorHandlerTests.eleventhPlayerIsRejected` | FR-02-04, FR-05, FR-14 |
| `RestErrorHandlerTests.duplicateUsernameIgnoringCaseIsRejected` | FR-02-05, FR-03, FR-14 |
| `RestErrorHandlerTests.joiningAStartedGameIsRejected` | FR-02-03, FR-10-05, FR-14 |
| `RestErrorHandlerTests.themeLength0IsRejected` | FR-01-01, FR-04, FR-14 |
| `RestErrorHandlerTests.whitespaceOnlyThemeIsRejected` | FR-01-01, FR-04, FR-14 |
| `RestErrorHandlerTests.themeLength1IsAccepted` | FR-01-01, FR-04, FR-14 |
| `RestErrorHandlerTests.themeLength20IsAccepted` | FR-01-01, FR-04, FR-14 |
| `RestErrorHandlerTests.themeLength21IsRejected` | FR-01-01, FR-04, FR-14 |
| `RestErrorHandlerTests.usernameLength0IsRejected` | FR-01-01, FR-03, FR-14 |
| `RestErrorHandlerTests.usernameLength1IsAccepted` | FR-01-01, FR-03, FR-14 |
| `RestErrorHandlerTests.usernameLength16IsAccepted` | FR-01-01, FR-03, FR-14 |
| `RestErrorHandlerTests.usernameLength17IsRejected` | FR-01-01, FR-03, FR-14 |
| `RestErrorHandlerTests.usernameWithInvalidCharacterIsRejected` | FR-01-01, FR-03, FR-14 |
| `RestErrorHandlerTests.missingUsernameIsRejected` | FR-01-01, FR-03, FR-14 |
| `WebSocketErrorHandlerTests.successfulActionSendsNoErrorFrame` | FR-14 |
| `WebSocketErrorHandlerTests.apiExceptionBecomesErrorFrameWithDefaultMessage` | FR-14 |
| `WebSocketErrorHandlerTests.apiExceptionCustomMessageIsUsedAsDetail` | FR-14 |
| `WebSocketErrorHandlerTests.unexpectedRuntimeExceptionBecomesInternalWithoutLeakingMessage` | NFR-12 |
| `WebSocketErrorHandlerTests.checkedExceptionBecomesInternalAndDoesNotPropagate` | NFR-07 |
| `WebSocketErrorHandlerTests.closedSessionIsNotSentTo` | NFR-07 |
| `WebSocketErrorHandlerTests.sendFailureIsSwallowed` | NFR-07 |
| `WebSocketErrorHandlerTests.errorsNeverCloseTheSession` | NFR-07 |
| `WebSocketErrorHandlerTests.everyCodeIsSentByName` | FR-14, NFR-14 |

## Tests not traced to a completed requirement

| Test | Notes |
| --- | --- |
| `FakeArtistApplicationTests.roundResultsFeatureIsDisabledByDefault`, `LobbyHandlerFeatureFlagConfigurationTests.configurationOverrideEnablesRoundResultsMessage`, `LobbyHandlerFeatureFlagTests.disabledRoundResultsFlagDoesNotSendExperimentalMessage` | Round-results feature flag – no requirement exists (`LobbyHandlerFeatureFlagTests.disabledRoundResultsFlagDoesNotSendExperimentalMessage` incidentally touches FR-09-05/FR-10-03) |
| `FakeArtistApplicationTests.kickPlayerFeatureIsDisabledByDefault`, `LobbyHandlerKickTests.disabledKickFlagRejectsKickWithoutChangingGame`, `LobbyHandlerKickTests.nonHostCannotKickAnotherPlayer`, `LobbyHandlerKickTests.hostCannotKickUnknownOrHostTarget`, `LobbyHandlerKickTests.enabledHostKickRemovesTargetAndNotifiesBothSessions`, `LobbyHandlerErrorTests.hostKickingThemselfIsRejected` | FR-16 kick – requirement still **Planned** |
| `FakeArtistApplicationTests.hostEndpointRespondsWithinPerformanceThreshold` | NFR-11 – only **Partially implemented** |
| `RestErrorHandlerTests.missingStaticResourceIsQuiet404`, `RestErrorHandlerUnexpectedErrorTests.unexpectedExceptionReturnsInternalWithoutLeakingMessage`, `RestErrorHandlerTests.validationLogNamesFieldButNotValue`, `RestErrorHandlerTests.expectedErrorIsLoggedWithoutStackTrace`, `RestErrorHandlerUnexpectedErrorTests.unexpectedExceptionIsLoggedAtErrorWithStackTrace`, `WebSocketErrorHandlerTests.unexpectedRuntimeExceptionBecomesInternalWithoutLeakingMessage` | NFR-12 logging – only **Partially implemented** |
| `ErrorCodeTests.everyCodeMapsToAnErrorStatus`, `ErrorCodeTests.onlyServerFailuresAre5xx` | NFR-14 – only **Partially implemented** (also support FR-14) |
| `GameControllerUnitTests.removePlayerRejectsUnknownPlayer` | No matching requirement (closest: NFR-02, partial) |
