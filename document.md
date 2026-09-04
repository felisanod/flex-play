# flex-player - Complete API & Types Documentation

> **flex-player** is a third-party Android YouTube Music client written in Kotlin, using Jetpack Compose (Material 3). This document catalogs every external API, internal service, data type, and function used across the project.

---

## Table of Contents

1. [YouTube Music / InnerTube API (innertube module)](#1-youtube-music--innertube-api)
2. [OpenRouter AI Translation API](#2-openrouter-ai-translation-api)
3. [DeepL Translation API](#3-deepl-translation-api)
4. [BetterLyrics API](#4-betterlyrics-api)
5. [KuGou Lyrics API](#5-kugou-lyrics-api)
6. [LrcLib Lyrics API](#6-lrclib-lyrics-api)
7. [Paxsenix / Apple Music Lyrics API](#7-paxsenix--apple-music-lyrics-api)
8. [Last.fm Scrobbling API](#8-lastfm-scrobbling-api)
9. [Shazam Music Recognition API](#9-shazam-music-recognition-api)
10. [Discord Rich Presence API](#10-discord-rich-presence-api)
11. [Listen Together WebSocket API](#11-listen-together-websocket-api)
12. [GitHub API (Updater & AutoEq)](#12-github-api)
13. [YouTube BotGuard / PO Token API](#13-youtube-botguard--po-token-api)
14. [Return YouTube Dislike API](#14-return-youtube-dislike-api)
15. [LyricsPlus Mirrors API](#15-lyricsplus-mirrors-api)
16. [Core Domain Types (YTItem hierarchy)](#16-core-domain-types)
17. [Internal Services & Managers](#17-internal-services--managers)
18. [Database Layer (Room)](#18-database-layer)
19. [Dependency Injection (Hilt)](#19-dependency-injection)
20. [Key Constants & Preferences](#20-key-constants--preferences)

---

## 1. YouTube Music / InnerTube API

**Module:** `:innertube`  
**Package:** `com.flex-player.innertube`  
**Library:** `innertubex` (via JitPack `com.github.flex-playerGroup.innertubex:v0.5.2`)  
**Base URL:** `https://music.youtube.com/youtubei/v1/`

### 1.1 InnerTube Class

Thin compatibility facade wrapping `InnerTubeX`. Constructs a Ktor `HttpClient` (OkHttp engine) with content negotiation, gzip/deflate compression, connection pooling, timeouts, proxy support, and HTTP/2.

#### Properties
| Property | Type | Description |
|---|---|---|
| `locale` | `YouTubeLocale` | Language/region for requests |
| `visitorData` | `String?` | Anonymous visitor identifier |
| `dataSyncId` | `String?` | Data sync identifier |
| `authUser` | `String` | Authenticated user ID |
| `cookie` | `String?` | Browser cookie for auth |
| `proxy` | `Proxy?` | HTTP proxy configuration |
| `proxyAuth` | `String?` | Proxy authentication |
| `useLoginForBrowse` | `Boolean` | Force login for browse requests |

#### Methods
| Method | Signature | Description |
|---|---|---|
| `search` | `(client, query?, params?, continuation?)` | Search YouTube Music |
| `player` | `(client, videoId, playlistId?, signatureTimestamp?, poToken?)` | Get player/streaming data |
| `browse` | `(client, browseId, params?, continuation?, setLogin?)` | Browse pages (home, library, etc.) |
| `next` | `(client, videoId?, playlistId?, playlistSetVideoId?, index?, params?, continuation?)` | Watch-next / queue |
| `getSearchSuggestions` | `(client, input)` | Search autocomplete |
| `getQueue` | `(client, videoIds, playlistId?)` | Queue retrieval |
| `getTranscript` | `(client, videoId)` | Video transcript |
| `feedback` | `(client, tokens)` | Feedback/library tokens |
| `accountMenu` | `(client)` | Account menu |
| `accountsList` | `()` | List all accounts |
| `likeVideo` / `unlikeVideo` | `(client, videoId)` | Like/unlike video |
| `likePlaylist` / `unlikePlaylist` | `(client, playlistId)` | Like/unlike playlist |
| `subscribeChannel` / `unsubscribeChannel` | `(client, channelId, params?)` | Subscribe/unsubscribe |
| `addToPlaylist` | `(client, playlistId, videoId)` | Add video to playlist |
| `addPlaylistToPlaylist` | `(client, playlistId, addedPlaylistId)` | Add playlist to playlist |
| `removeFromPlaylist` | `(client, playlistId, videoId, setVideoId?)` | Remove from playlist |
| `moveSongPlaylist` | `(client, playlistId, setVideoId, successorSetVideoId)` | Reorder in playlist |
| `createPlaylist` | `(client, title)` | Create new playlist |
| `renamePlaylist` | `(client, playlistId, name)` | Rename playlist |
| `setPlaylistThumbnail` | `(client, playlistId, image)` | Set playlist thumbnail |
| `removePlaylistThumbnail` | `(client, playlistId)` | Remove playlist thumbnail |
| `deletePlaylist` | `(client, playlistId)` | Delete playlist |
| `uploadSong` | `(filename, contentLength, content)` | Upload song to library |
| `deletePrivatelyOwnedEntity` | `(entityId)` | Delete uploaded entity |
| `registerPlayback` | `(url, cpn, playlistId?, client?)` | Register playback tracking |
| `returnYouTubeDislike` | `(videoId)` | Get dislike count |
| `getMediaInfo` | `(videoId): Result<MediaInfo>` | Combined next+dislike info |
| `fetchFreshVisitorData` | `()` | New visitor data |

### 1.2 YouTube Object (High-Level API)

Main public entry point used throughout the app.

#### Search Functions
| Function | Return Type | Description |
|---|---|---|
| `searchSuggestions(query)` | `Result<SearchSuggestions>` | Autocomplete suggestions |
| `searchSummary(query)` | `Result<SearchSummaryPage>` | Grouped search results |
| `search(query, filter?)` | `Result<SearchResult>` | Filtered search |
| `searchContinuation(continuation)` | `Result<SearchResult>` | Load more results |

#### Content Functions
| Function | Return Type | Description |
|---|---|---|
| `album(browseId, withSongs?)` | `Result<AlbumPage>` | Album detail + songs |
| `albumSongs(playlistId, album?)` | `Result<List<SongItem>>` | Paginate album songs |
| `artist(browseId)` | `Result<ArtistPage>` | Artist page |
| `artistItems(endpoint)` | `Result<ArtistItemsPage>` | Artist's songs/albums/etc. |
| `artistItemsContinuation(continuation)` | `Result<ArtistItemsContinuationPage>` | More artist items |
| `playlist(playlistId)` | `Result<PlaylistPage>` | Playlist with songs |
| `playlistContinuation(continuation)` | `Result<PlaylistContinuationPage>` | More playlist songs |
| `podcast(podcastId)` | `Result<PodcastPage>` | Podcast episodes |
| `home(continuation?, params?)` | `Result<HomePage>` | Home feed |
| `explore()` | `Result<ExplorePage>` | Explore page |
| `newReleaseAlbums()` | `Result<List<AlbumItem>>` | New releases |
| `moodAndGenres()` | `Result<List<MoodAndGenres>>` | Mood/genre categories |
| `browse(browseId, params?)` | `Result<BrowseResult>` | Generic browse |
| `library(browseId, tabIndex?)` | `Result<LibraryPage>` | Library page |
| `libraryContinuation(continuation)` | `Result<LibraryContinuationPage>` | More library items |
| `getChartsPage(continuation?)` | `Result<ChartsPage>` | Charts |
| `musicHistory()` | `Result<HistoryPage>` | Play history |

#### Player & Playback Functions
| Function | Return Type | Description |
|---|---|---|
| `player(videoId, playlistId?, client?, signatureTimestamp?, poToken?)` | `Result<PlayerResponse>` | Streaming data |
| `registerPlayback(playlistId, playbackTracking)` | `Result<Boolean>` | Track playback |
| `next(endpoint, continuation?)` | `Result<NextResult>` | Queue/related/lyrics endpoints |
| `lyrics(endpoint)` | `Result<String?>` | Plain lyrics text |
| `related(endpoint)` | `Result<RelatedPage>` | Related content |
| `queue(videoIds, playlistId?)` | `Result<List<SongItem>>` | Queue data |
| `transcript(videoId)` | `Result<String>` | Timestamped transcript |

#### Library Mutation Functions
| Function | Description |
|---|---|
| `likeVideo(videoId, like)` | Like/unlike |
| `likePlaylist(playlistId, like)` | Like/unlike |
| `subscribeChannel(channelId, subscribe, params?)` | Subscribe/unsubscribe |
| `savePodcast(podcastId, save)` | Save/unsave podcast |
| `addEpisodeToSavedEpisodes(videoId)` | Save episode |
| `removeEpisodeFromSavedEpisodes(videoId, setVideoId?)` | Remove episode |
| `addSongToLibrary(videoId)` | Add to library |
| `removeSongFromLibrary(videoId)` | Remove from library |
| `toggleSongLibrary(videoId, addToLibrary)` | Toggle library state |

#### Playlist Mutation Functions
| Function | Description |
|---|---|
| `addToPlaylist(playlistId, videoId)` | Add to playlist |
| `addPlaylistToPlaylist(playlistId, addedPlaylistId)` | Nest playlist |
| `removeFromPlaylist(playlistId, videoId, setVideoId?)` | Remove from playlist |
| `moveSongPlaylist(playlistId, setVideoId, successorSetVideoId)` | Reorder |
| `createPlaylist(title)` | Create playlist |
| `renamePlaylist(playlistId, name)` | Rename |
| `uploadCustomThumbnailLink(playlistId, image)` | Set thumbnail |
| `removeThumbnailPlaylist(playlistId)` | Remove thumbnail |
| `deletePlaylist(playlistId)` | Delete |

#### Account Functions
| Function | Return Type | Description |
|---|---|---|
| `accountInfo()` | `Result<AccountInfo>` | Current account info |
| `accountsList()` | `Result<List<YouTubeAccount>>` | All accounts |
| `visitorData()` | `Result<String>` | Fresh visitor data |

#### Upload Functions
| Function | Description |
|---|---|
| `uploadSong(filename, contentLength, content, onProgress)` | Upload song (max 300MB) |
| `deleteUploadedSong(entityId)` | Delete uploaded song |

#### SearchFilter Value Class
| Constant | Filter |
|---|---|
| `FILTER_SONG` | Songs only |
| `FILTER_VIDEO` | Videos only |
| `FILTER_ALBUM` | Albums only |
| `FILTER_ARTIST` | Artists only |
| `FILTER_FEATURED_PLAYLIST` | Featured playlists |
| `FILTER_COMMUNITY_PLAYLIST` | Community playlists |
| `FILTER_PODCAST` | Podcasts |
| `FILTER_EPISODE` | Episodes |
| `FILTER_PROFILE` | Profiles |

### 1.3 Response Models (`models/response/`)

| Model | Description |
|---|---|
| `PlayerResponse` | Streaming URLs, formats, adaptive formats, video details, playback tracking |
| `BrowseResponse` | Browse page contents, headers, continuation, microformat |
| `SearchResponse` | Tabbed search results with continuation |
| `NextResponse` | Watch-next results, queue, tabs |
| `AccountMenuResponse` | Account name, email, channel handle, photo |
| `ContinuationResponse` | Pagination continuation items |
| `CreatePlaylistResponse` | New playlist ID |
| `EditPlaylistResponse` | Updated header |
| `FeedbackResponse` | Feedback processing status |
| `GetQueueResponse` | Queue data with playlist panel |
| `GetSearchSuggestionsResponse` | Search suggestion sections |
| `GetTranscriptResponse` | Transcript cue groups with timestamps |

### 1.4 Renderer Models (`models/`)

| Model | Description |
|---|---|
| `MusicResponsiveListItemRenderer` | List item (song/album/artist/playlist/podcast/episode) |
| `MusicTwoRowItemRenderer` | Grid/card item |
| `MusicCarouselShelfRenderer` | Horizontal carousel section |
| `MusicCardShelfRenderer` | Card shelf (top result) |
| `MusicShelfRenderer` | Vertical shelf (song list) |
| `MusicPlaylistShelfRenderer` | Playlist content shelf |
| `MusicResponsiveHeaderRenderer` | Page header with buttons |
| `MusicEditablePlaylistDetailHeaderRenderer` | Editable playlist header |
| `MusicDescriptionShelfRenderer` | Description text block |
| `MusicNavigationButtonRenderer` | Navigation button (mood/genre) |
| `MusicMultiRowListItemRenderer` | Multi-row item (podcast episodes) |
| `MusicQueueRenderer` | Queue display |
| `PlaylistPanelRenderer` | Queue panel (autoplay/related) |
| `PlaylistPanelVideoRenderer` | Individual queue item |
| `GridRenderer` | Grid layout |
| `SectionListRenderer` | Section list with chips |
| `Tabs` / `Tab` | Tabbed layout |
| `TwoColumnBrowseResultsRenderer` | Two-column layout |

### 1.5 Domain Models (`models/`)

| Model | Properties | Description |
|---|---|---|
| `SongItem` | id, title, artists, album?, duration?, musicVideoType?, thumbnail, explicit, endpoint?, setVideoId?, libraryAddToken?, libraryRemoveToken?, historyRemoveToken?, isEpisode?, uploadEntityId? | Song/track |
| `AlbumItem` | browseId, playlistId, title, artists?, year?, thumbnail, explicit | Album |
| `PlaylistItem` | id, title, author?, songCountText?, thumbnail?, playEndpoint?, shuffleEndpoint?, radioEndpoint?, isEditable?, isPodcast?, description?, authorAvatarUrl? | Playlist |
| `ArtistItem` | id, title, thumbnail?, channelId?, playEndpoint?, shuffleEndpoint?, radioEndpoint?, isProfile? | Artist |
| `PodcastItem` | id, title, author?, episodeCountText?, thumbnail?, playEndpoint?, shuffleEndpoint?, libraryAddToken?, libraryRemoveToken?, channelId? | Podcast show |
| `EpisodeItem` | id, title, author?, podcast?, duration?, publishDateText?, thumbnail, explicit, endpoint?, libraryAddToken?, libraryRemoveToken?, markAsPlayedToken?, markAsUnplayedToken? | Podcast episode |
| `AccountInfo` | name, email?, channelHandle?, thumbnailUrl? | User account |
| `MediaInfo` | videoId, title?, author?, authorId?, authorThumbnail?, description?, uploadDate?, subscribers?, viewCount?, like?, dislike? | Video metadata |
| `Artist` | name, id? | Artist reference |
| `Album` | name, id | Album reference |

### 1.6 Page Models (`pages/`)

| Page | Description |
|---|---|
| `AlbumPage` | Album + songs + other versions |
| `ArtistPage` | Artist + sections + description + subscriber count |
| `ArtistItemsPage` / `ArtistItemsContinuationPage` | Artist's content items |
| `HomePage` | Chips + carousel sections |
| `SearchResult` | Search items + continuation |
| `SearchSummaryPage` | Grouped search (top result, songs, albums, etc.) |
| `PlaylistPage` | Playlist + songs + continuation |
| `PlaylistContinuationPage` | More playlist songs |
| `PodcastPage` | Podcast + episodes + continuation |
| `LibraryPage` / `LibraryContinuationPage` | Library items |
| `ChartsPage` | Chart sections (trending/top/genre/new releases) |
| `ExplorePage` | New release albums + mood/genres |
| `HistoryPage` | Playback history sections |
| `NextResult` | Queue items + lyrics/related endpoints |
| `RelatedPage` | Related songs/albums/artists/playlists |
| `BrowseResult` | Generic browse results |
| `MoodAndGenres` | Mood/genre categories |

### 1.7 Utility Types

| Type | Description |
|---|---|
| `YouTubeUrlParser` | Parses YouTube URLs into `Video`, `Playlist`, `Album`, or `Artist` |
| `PageHelper` | Extracts artists, duration, library tokens from renderers |
| `UploadProgressInputStream` | Tracks upload progress (0..1) |
| `SearchFilter` | Value class with URL-encoded filter params |

---

## 2. OpenRouter AI Translation API

**Package:** `com.flex-player.music.api`  
**Base URL:** `https://openrouter.ai/api/v1` (configurable)  
**Protocol:** HTTP/REST (JSON)  
**Purpose:** AI-powered lyrics translation, romanization, and transliteration

### 2.1 OpenRouterService

Synchronous translation with retry logic.

| Function | Signature | Description |
|---|---|---|
| `translate` | `suspend (text, targetLanguage, apiKey, baseUrl, model, mode, maxRetries, customSystemPrompt): Result<List<String>>` | Translates lyrics line-by-line |

**Modes:** `"Romanized"`, `"Transcribed"`, or standard translation  
**Features:** Exponential backoff retry (max 3), JSON schema structured output, OpenRouter provider routing

### 2.2 OpenRouterStreamingService

Streaming translation via SSE.

| Function | Signature | Description |
|---|---|---|
| `streamTranslation` | `(text, targetLanguage, apiKey, baseUrl, model, mode, customSystemPrompt): Flow<StreamChunk>` | Streams translation chunks |

**StreamChunk types:**
- `Content(text: String)` — partial chunk
- `Complete(translatedLines: List<String>)` — final parsed result
- `Error(message: String)` — error

---

## 3. DeepL Translation API

**Package:** `com.flex-player.music.api`  
**Base URL:** `https://api-free.deepl.com/v2/translate` (free) or `https://api.deepl.com/v2/translate` (pro)  
**Protocol:** HTTP/REST (JSON)  
**Purpose:** Professional-grade lyrics translation

### DeepLService

| Function | Signature | Description |
|---|---|---|
| `translate` | `suspend (text, targetLanguage, apiKey, formality?, maxRetries?): Result<List<String>>` | Translates lyrics with formality control |

**Features:** Auto-detects free/pro API from key suffix (`:fx`), retries on 5xx errors, line count padding

---

## 4. BetterLyrics API

**Package:** `com.flex-player.music.betterlyrics`  
**Base URL:** `https://lyrics-api.boidu.dev`  
**Protocol:** HTTP/REST (TTML)  
**Purpose:** Synced lyrics with timing data

### BetterLyrics

| Function | Signature | Description |
|---|---|---|
| `getLyrics` | `suspend (title, artist, duration, album?): Result<String>` | Fetches TTML lyrics, converts to LRC format |

**Internal:** `fetchTTML(artist, title, duration, album): String?` — HTTP GET `/getLyrics` with params `s`, `a`, `d`, `al`

### BetterLyricsProvider

| Function | Description |
|---|---|
| `getLyrics(title, artist, duration, album?)` | Provider interface implementation |

---

## 5. KuGou Lyrics API

**Package:** `com.flex-player.kugou`  
**Base URLs:** `https://mobileservice.kugou.com` (search), `https://lyrics.kugou.com` (lyrics)  
**Protocol:** HTTP/REST (JSON)  
**Purpose:** Chinese lyrics search and download

### KuGou

| Function | Signature | Description |
|---|---|---|
| `getLyrics` | `suspend (title, artist, duration, album?): Result<String>` | Search + download LRC lyrics |
| `getAllPossibleLyricsOptions` | `suspend (title, artist, duration, album?, callback)` | All matching lyrics variants |
| `getLyricsCandidate` | `suspend (keyword, duration): Candidate?` | Best lyrics candidate |
| `searchSongs` | `suspend (keyword): SearchSongResponse` | Search songs by keyword |
| `generateKeyword` | `(title, artist, album?): Keyword` | Normalize search keyword |

**Internal endpoints:**
- `GET https://mobileservice.kugou.com/api/v3/search/song` — song search
- `GET https://lyrics.kugou.com/search` — lyrics search (by keyword or hash)
- `GET https://lyrics.kugou.com/download` — download LRC content (Base64 encoded)

### Models
- `SearchSongResponse` — song search results
- `SearchLyricsResponse` — lyrics candidates
- `DownloadLyricsResponse` — Base64-encoded LRC content
- `Keyword` — normalized search keyword (title, artist, album)

---

## 6. LrcLib Lyrics API

**Package:** `com.flex-player.lrclib`  
**Base URL:** `https://lrclib.net`  
**Protocol:** HTTP/REST (JSON)  
**Purpose:** Open-source synced lyrics database

### LrcLib

| Function | Signature | Description |
|---|---|---|
| `getLyrics` | `suspend (title, artist, duration, album?): Result<String>` | Best-matching synced/plain lyrics |
| `getAllLyrics` | `suspend (title, artist, duration, album?, callback)` | All matching lyrics (up to 4) |
| `lyrics` | `suspend (artist, title): Result<List<Track>>` | Raw track search |

**Search strategy:** 5-step fallback — cleaned title+artist → title only → query combined → query title → original title

**Model:** `Track` (trackName, artistName, albumName, duration, syncedLyrics?, plainLyrics?)

---

## 7. Paxsenix / Apple Music Lyrics API

**Package:** `com.flex-player.paxsenix`  
**Base URLs:** `https://lyrics.paxsenix.org` (lyrics proxy), `https://amp-api.music.apple.com` (search)  
**Protocol:** HTTP/REST (JSON)  
**Purpose:** Apple Music synced lyrics (syllable-level timing)

### Paxsenix

| Function | Signature | Description |
|---|---|---|
| `init` | `(context)` | Initialize HTTP client + Apple token |
| `getLyrics` | `suspend (title, artist, duration, album?): Result<String>` | Best-matching synced lyrics |
| `getAllLyrics` | `suspend (title, artist, duration, album?, callback)` | All matching variants |

**Internal:**
- `search(query): List<SearchResult>` — Apple Music catalog search
- `fetchLyricsForTrack(id): Result<String>` — TTML/ELRC/plain lyrics
- `scoreAndFilterResults(...)` — Duration/title/artist scoring with version mismatch penalties
- `convertTTMLToAppFormat(ttml)` — TTML → LRC conversion via `TTMLParser`
- `AppleTokenManager` — Scrapes JWT from `https://beta.music.apple.com`

**Models:**
- `AppleMusicSearchResponse` — Apple Music catalog search results
- `LyricsResponse` — lyrics with TTML, ELRC, and plain variants
- `SearchResult` — id, trackName, artistName, albumName, duration, artwork

---

## 8. Last.fm Scrobbling API

**Package:** `com.flex-player.lastfm`  
**Base URL:** `https://ws.audioscrobbler.com/2.0/`  
**Protocol:** HTTP/POST (form-urlencoded, MD5 API signature)  
**Purpose:** Scrobbling, now-playing updates, love/unlove tracks

### LastFM

| Function | Signature | Description |
|---|---|---|
| `initialize` | `(apiKey, secret)` | Set API credentials |
| `isInitialized` | `(): Boolean` | Check if credentials are set |
| `getToken` | `suspend (): Result<TokenResponse>` | OAuth token (legacy) |
| `getSession` | `suspend (token): Result<Authentication>` | OAuth session (legacy) |
| `getMobileSession` | `suspend (username, password): Authentication` | Mobile session auth |
| `updateNowPlaying` | `suspend (artist, track, album?, trackNumber?, duration?)` | Update now playing |
| `scrobble` | `suspend (artist, track, timestamp, album?, trackNumber?, duration?)` | Scrobble track |
| `setLoveStatus` | `suspend (artist, track, love)` | Love/unlove track |

**Auth:** MD5 API signature — sorted params + secret → MD5 hash  
**Constants:** `DEFAULT_SCROBBLE_DELAY_PERCENT = 0.5f`, `DEFAULT_SCROBBLE_MIN_SONG_DURATION = 30`, `DEFAULT_SCROBBLE_DELAY_SECONDS = 180`

### Models
- `Authentication` — session key, name
- `TokenResponse` — OAuth token
- `LastFmError` — error code + message

---

## 9. Shazam Music Recognition API

**Package:** `com.flex-player.shazamkit`  
**Base URL:** `https://amp.shazam.com/discovery/v5/en/US/android/-/tag/`  
**Protocol:** HTTP/POST (JSON)  
**Purpose:** Music recognition via audio fingerprinting

### Shazam

| Function | Signature | Description |
|---|---|---|
| `recognize` | `suspend (signature, sampleDurationMs): Result<RecognitionResult>` | Recognize song from fingerprint |
| `clearCache` | `()` | Clear result cache |

**Features:** Rate limiting (1s between requests), exponential backoff retry (max 3), 5-minute result cache, randomized user agents and timezones

### MusicRecognitionService

| Function | Signature | Description |
|---|---|---|
| `hasRecordPermission` | `(context): Boolean` | Check mic permission |
| `recognize` | `suspend (context): RecognitionStatus` | Full recognition pipeline |
| `reset` | `()` | Reset state |

**Pipeline:** Record audio (12s @ 44.1kHz) → Resample to 16kHz mono → Generate VibraSignature fingerprint → Send to Shazam API

### Models
- `RecognitionResult` — trackId, title, artist, album, coverArtUrl, coverArtHqUrl, genre, releaseDate, label, lyrics, shazamUrl, appleMusicUrl, spotifyUrl, isrc, youtubeVideoId
- `ShazamRequestJson` — geolocation, signature (samplems, timestamp, uri), timestamp, timezone
- `ShazamResponseJson` — track details, hub links, metadata
- `RecognitionStatus` — Ready, Listening, Processing, Success, Error, NoMatch

---

## 10. Discord Rich Presence API

**Package:** `com.flex-player.music.discord`  
**Base URLs:** `https://discord.com/api/v10` (REST), `wss://gateway.discord.gg/?v=10` (WebSocket)  
**Protocol:** HTTP/REST + WebSocket  
**Purpose:** Discord Rich Presence (show currently playing track)

### 10.1 DiscordAuth

OAuth2 PKCE authentication flow.

| Function | Signature | Description |
|---|---|---|
| `authorize` | `suspend (activity): DiscordAuthResult` | Full OAuth2 flow with Chrome Custom Tabs |
| `cancel` | `()` | Cancel pending auth |
| `refresh` | `suspend (refreshToken): DiscordAuthResult` | Refresh access token |
| `close` | `()` | Close HTTP client |
| `generatePkcePair` | `(): PkcePair` | Generate PKCE verifier + challenge |

**Endpoints:**
- `POST https://discord.com/api/v10/oauth2/token` — token exchange

### 10.2 DiscordGateway

WebSocket connection to Discord Gateway v10.

| Function | Signature | Description |
|---|---|---|
| `connect` | `suspend ()` | Open WebSocket connection |
| `close` | `(code, reason?)` | Close connection |
| `send` | `(frameJson)` | Send JSON frame |
| `identify` | `suspend (token)` | IDENTIFY frame |
| `presenceUpdate` | `(presenceJson)` | Update presence |
| `resume` | `suspend (sessionId, seq, token)` | Resume session |
| `heartbeat` | `(seq)` | Heartbeat frame |

**Gateway Opcodes:** DISPATCH(0), HEARTBEAT(1), IDENTIFY(2), PRESENCE_UPDATE(3), RESUME(6), RECONNECT(7), INVALID_SESSION(9), HELLO(10), HEARTBEAT_ACK(11)

**Events:** `Hello`, `Ready`, `Resumed`, `HeartbeatAck`, `InvalidSession`, `Disconnected`, `RefreshToken`, `TextDispatch`

### 10.3 DiscordRpcManager

| Function | Description |
|---|---|
| `connect` | Initialize auth + gateway |
| `disconnect` | Clean disconnect |
| `updatePresence` | Update activity with template rendering |
| `setAssets` | Cache external asset URLs |

### 10.4 Supporting Types
- `DiscordActivity` — details, state, timestamps, assets, buttons
- `DiscordActivityBuilder` — fluent builder for activity
- `DiscordTemplateRenderer` — renders `{title}`, `{artist}`, `{album}`, etc.
- `DiscordExternalAssets` — resolves image keys to CDN URLs
- `DiscordTokenStore` — encrypted token persistence
- `DiscordReconnectStrategy` — close code → reconnect decision
- `DiscordSuperProperties` — base64-encoded client properties

---

## 11. Listen Together WebSocket API

**Package:** `com.flex-player.music.listentogether`  
**Default Server:** `wss://metroserverx.meowery.eu/ws`  
**Protocol:** WebSocket (Protobuf binary)  
**Purpose:** Real-time synchronized listening rooms

### 11.1 ListenTogetherClient

| Function | Signature | Description |
|---|---|---|
| `connect` | `()` | Connect to server |
| `disconnect` | `()` | Disconnect and clear state |
| `createRoom` | `(username)` | Create a new room |
| `joinRoom` | `(roomCode, username)` | Join existing room |
| `leaveRoom` | `()` | Leave current room |
| `kickUser` | `(userId)` | Kick user (host only) |
| `approveJoin` | `(userId)` | Approve join request |
| `rejectJoin` | `(userId, reason?)` | Reject join request |
| `approveSuggestion` | `(suggestionId)` | Approve track suggestion |
| `rejectSuggestion` | `(suggestionId)` | Reject track suggestion |
| `sendPlaybackAction` | `(action)` | Sync play/pause/seek/skip |
| `sendTrackChange` | `(trackId, trackInfo, position, isPlaying)` | Change track |
| `suggestTrack` | `(trackInfo)` | Suggest track to host |
| `sendVolumeSync` | `(volume)` | Sync volume |
| `blockUser` | `(username)` | Block user |
| `unblockUser` | `(username)` | Unblock user |

**State Flows:** `connectionState`, `roomState`, `role`, `userId`, `pendingJoinRequests`, `bufferingUsers`, `pendingSuggestions`, `blockedUsernames`, `logs`, `events`

### 11.2 Message Types & Payloads

| Message Type | Payload | Direction |
|---|---|---|
| `CREATE_ROOM` | `CreateRoomPayload(username)` | Client → Server |
| `JOIN_ROOM` | `JoinRoomPayload(roomCode, username)` | Client → Server |
| `LEAVE_ROOM` | — | Client → Server |
| `ROOM_CREATED` | `RoomCreatedPayload(roomCode, userId, sessionToken)` | Server → Client |
| `JOIN_REQUEST` | `JoinRequestPayload(userId, username)` | Server → Client |
| `JOIN_APPROVED` | `JoinApprovedPayload(userId, roomCode, sessionToken, state)` | Server → Client |
| `JOIN_REJECTED` | `JoinRejectedPayload(reason)` | Server → Client |
| `USER_JOINED` | `UserJoinedPayload(userId, username)` | Server → Client |
| `USER_LEFT` | `UserLeftPayload(userId, username)` | Server → Client |
| `PLAYBACK_ACTION` | `PlaybackActionPayload(action, position?, trackId?)` | Bidirectional |
| `TRACK_CHANGE` | `TrackChangePayload(trackId, trackInfo, position, isPlaying)` | Host → Guests |
| `TIME_SYNC` / `TIME_SYNC_RESPONSE` | `PingPayload`, `TimeSyncResponsePayload` | Bidirectional |
| `SUGGESTION` | `SuggestionPayload(trackInfo)` | Guest → Host |
| `SUGGESTION_RECEIVED` | `SuggestionReceivedPayload(suggestionId, fromUsername, trackInfo)` | Server → Client |
| `SUGGESTION_DECISION` | `SuggestionDecisionPayload(suggestionId, approved)` | Host → Server |
| `VOLUME_SYNC` | `VolumeSyncPayload(volume)` | Bidirectional |
| `RECONNECT` | `ReconnectPayload(sessionToken)` | Client → Server |
| `KICKED` | `KickedPayload(reason)` | Server → Client |
| `ERROR` | `ErrorPayload(code, message)` | Server → Client |

### 11.3 Supporting Types
- `ConnectionState` — DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
- `RoomRole` — HOST, GUEST, NONE
- `RoomState` — roomCode, hostId, users, isPlaying, position, lastUpdate, volume, revision
- `UserInfo` — userId, username, isHost
- `ServerClock` — drift-compensated server time
- `MessageCodec` — Protobuf encode/decode with compression
- `ListenTogetherServers` — server registry

---

## 12. GitHub API

### 12.1 App Updater

**Package:** `com.flex-player.music.utils`  
**Base URL:** `https://api.github.com/repos/flex-playerGroup/flex-player`  
**Purpose:** Check for app updates

| Function | Signature | Description |
|---|---|---|
| `getLatestRelease` | `suspend (forceRefresh?): Result<ReleaseInfo>` | Latest release |
| `getAllReleases` | `suspend (forceRefresh?): Result<List<ReleaseInfo>>` | Paginated releases |
| `getLatestKmpRelease` | `suspend (): Result<ReleaseInfo?>` | KMP migration APK |
| `checkForUpdate` | `suspend (forceRefresh?): Result<Pair<ReleaseInfo?, Boolean>>` | Update check (2h cache) |
| `isUpdateAvailable` | `(currentVersion, latestVersion): Boolean` | Semantic version compare |
| `compareVersions` | `(v1, v2): Int` | Version comparison |
| `getDownloadUrlForCurrentVariant` | `(releaseInfo): String?` | Correct APK URL |

**Models:** `ReleaseInfo` (tagName, versionName, description, releaseDate, assets), `ReleaseAsset` (name, downloadUrl, size, architecture, variant)

### 12.2 AutoEq Search

**Package:** `com.flex-player.music.eq.data`  
**Base URL:** `https://api.github.com/repos/ndellagrotte/AutoEq`  
**Purpose:** Headphone EQ profiles

| Function | Signature | Description |
|---|---|---|
| `buildIndex` | `suspend (): Boolean` | Fetch GitHub tree, build search index |
| `searchModels` | `(query, maxResults?): Map<String, List<Entry>>` | Search headphone models |
| `getVariantsForModel` | `(modelName): List<Entry>` | All variants for model |
| `loadEQ` | `suspend (entry): ParametricEQ?` | Load parametric EQ data |
| `isDatabaseCached` | `(): Boolean` | Check if tree is cached |

**Internal endpoints:**
- `GET https://api.github.com/repos/ndellagrotte/AutoEq/git/trees/master?recursive=1` — file tree
- `GET https://raw.githubusercontent.com/ndellagrotte/AutoEq/master/...` — raw files

---

## 13. YouTube BotGuard / PO Token API

**Package:** `com.flex-player.music.utils.potoken`  
**Purpose:** Generate Proof of Origin tokens for YouTube playback

### PoTokenGenerator

| Function | Description |
|---|---|
| `generatePoToken` | Generate PO token using WebView-based BotGuard challenge |

**Internal endpoints:**
- `POST https://www.youtube.com/api/jnn/v1/Create` — BotGuard challenge creation
- `POST https://www.youtube.com/api/jnn/v1/GenerateIT` — Integrity token generation

**Supporting types:**
- `PoTokenWebView` — WebView-based JavaScript execution
- `PoTokenResult` — token + expiry
- `PoTokenException` — generation failures
- `JavaScriptUtil` — JavaScript asset loading

---

## 14. Return YouTube Dislike API

**Base URL:** `https://returnyoutubedislikeapi.com`  
**Protocol:** HTTP/REST (JSON, with retry)  
**Purpose:** Fetch dislike counts for videos

| Endpoint | Method | Parameters |
|---|---|---|
| `/Votes` | GET | `videoId` |

**Response:** `ReturnYouTubeDislikeResponse` (id, dateCreated, likes, dislikes, rating, viewCount, deleted)

---

## 15. LyricsPlus Mirrors API

**Package:** `com.flex-player.music.lyrics`  
**Mirrors:** `lyricsplus.binimum.org`, `lyricsplus.atomix.one`, `lyricsplus.prjktla.my.id`, `lyricsplus-seven.vercel.app`  
**Protocol:** HTTP/REST  
**Purpose:** Multi-source synced lyrics aggregation

### LyricsPlusProvider

| Function | Description |
|---|---|
| `getLyrics(title, artist, duration, album?)` | Fetch from mirrors with fallback |

---

## 16. Core Domain Types

### YTItem Hierarchy

```
sealed class YTItem
├── SongItem (id, title, artists, album?, duration?, thumbnail, explicit, ...)
├── AlbumItem (browseId, playlistId, title, artists?, year?, thumbnail, explicit)
├── PlaylistItem (id, title, author?, songCountText?, thumbnail?, ...)
├── ArtistItem (id, title, thumbnail?, channelId?, ...)
├── PodcastItem (id, title, author?, episodeCountText?, thumbnail?, ...)
└── EpisodeItem (id, title, author?, podcast?, duration?, thumbnail, ...)
```

**Extensions:**
- `List<T>.filterExplicit(enabled)` — filter explicit content
- `List<T>.filterVideoSongs(disableVideos)` — filter video-only songs
- `List<T>.filterYoutubeShorts(enabled)` — filter YouTube Shorts
- `List<T>.getContinuation()` — extract continuation token

### Endpoint Types

```
sealed class Endpoint
├── WatchEndpoint (videoId?, playlistId?, params?, index?)
├── BrowseEndpoint (browseId, params?)
│   ├── isArtistEndpoint, isAlbumEndpoint, isPlaylistEndpoint, isPodcastEndpoint
├── SearchEndpoint (params?, query)
├── FeedbackEndpoint (feedbackToken)
├── QueueAddEndpoint (queueInsertPosition, queueTarget)
├── ShareEntityEndpoint (serializedShareEntity)
├── DefaultServiceEndpoint (subscribeEndpoint?, feedbackEndpoint?)
└── ToggledServiceEndpoint (feedbackEndpoint?)
```

---

## 17. Internal Services & Managers

### Playback
- **`MusicService`** — Media3 `MediaSessionService` for background playback
- **`PlayerConnection`** — Connection to MusicService with StateFlow-based state
- **`ExoDownloadService`** — Media3 download service for offline caching

### Sync
- **`SyncUtils`** — Full YouTube Music library sync engine (1644 lines) with queued/coalesced operations

### Scrobbling
- **`ScrobbleManager`** — Manages Last.fm scrobbling with delay and state tracking

### Discord
- **`DiscordRpcManager`** — Manages Discord Rich Presence lifecycle
- **`DiscordTokenStore`** — Encrypted token storage
- **`DiscordTemplateRenderer`** — Template variable rendering

### Lyrics
- **`LyricsHelper`** — Orchestrates multi-provider lyrics fetching
- **`LyricsProviderRegistry`** — Registry of all lyrics providers
- **`LyricsTranslationHelper`** — AI translation orchestration
- **`LyricsUtils`** — LRC parsing, formatting, and manipulation

### Widgets
- **`flex-playerWidgetManager`** — Renders music widgets (compact, wide, full, turntable)
- **`PlaylistWidgetManager`** — Renders playlist widget with Quick Picks grid

### Recognition
- **`MusicRecognitionService`** — Full audio recording → fingerprint → recognition pipeline
- **`RecognitionForegroundService`** — Foreground service for background recognition
- **`ShazamSignatureGenerator`** / **`VibraSignature`** — Audio fingerprint generation
- **`AudioResampler`** — Audio format conversion (44.1kHz → 16kHz)

---

## 18. Database Layer

**Package:** `com.flex-player.music.db`  
**Technology:** Room (with KSP)

### DAOs
- **`InternalDatabaseDao`** — Songs, artists, albums, playlists, events, search history
- **`MetadataDao`** — Sync metadata (version, timestamps)

### Entities
- `SongEntity`, `ArtistEntity`, `AlbumEntity`, `PlaylistEntity`, `PlaylistSongMap`
- `EventEntity` — scrobble/play events
- `SearchHistoryEntity` — recent searches
- `LyricsEntity` — cached lyrics
- `DownloadEntity` — download state
- `SyncMetadataEntity` — sync state

### Database
- `MusicDatabase` — Room database (schemas in `app/schemas/`)

---

## 19. Dependency Injection

**Technology:** Hilt (Dagger)

### Modules
| Module | Provides |
|---|---|
| `AppModule` | Database, DAOs, caches, ListenTogether client/manager |
| `NetworkModule` | NetworkConnectivityObserver |
| `WrappedModule` | WrappedManager, WrappedAudioService |

### Qualifiers
- `@PlayerCache` — ExoPlayer cache
- `@DownloadCache` — Download cache
- `@ApplicationScope` — Application coroutine scope

---

## 20. Key Constants & Preferences

**Package:** `com.flex-player.music.constants`

### Preference Keys (DataStore)
Audio quality, Discord RPC settings, Listen Together config (server URL, username, auto-approval, sync volume, blocked users), Last.fm scrobbling settings, sorting/filtering enums, lyrics provider toggles (LrcLib/KuGou/BetterLyrics/Paxsenix/LyricsPlus), AI translation settings (OpenRouter/DeepL), sleep timer, search source, language/country codes.

### Media Session Constants
- `TOGGLE_LIBRARY`, `TOGGLE_LIKE`, `START_RADIO`, `TOGGLE_SHUFFLE`, `TOGGLE_REPEAT`, `ADD_TO_TARGET_PLAYLIST`

### Build Configuration
- `LASTFM_API_KEY`, `LASTFM_SECRET` — from GitHub Secrets
- `DISCORD_APP_ID = 1447278780795064401`
- `CAST_AVAILABLE` — flavor-dependent
- `UPDATER_AVAILABLE` — flavor-dependent
- `ARCHITECTURE = "universal"`

---

## Summary: All External API Endpoints

| # | Service | Base URL | Protocol | Files |
|---|---|---|---|---|
| 1 | YouTube Music InnerTube | `https://music.youtube.com/youtubei/v1/` | REST (protobuf) | `InnerTube.kt`, `YouTube.kt` |
| 2 | OpenRouter AI | `https://openrouter.ai/api/v1` | REST + SSE | `OpenRouterService.kt`, `OpenRouterStreamingService.kt` |
| 3 | DeepL | `https://api-free.deepl.com/v2/translate` | REST | `DeepLService.kt` |
| 4 | BetterLyrics | `https://lyrics-api.boidu.dev` | REST (TTML) | `BetterLyrics.kt` |
| 5 | KuGou | `https://mobileservice.kugou.com`, `https://lyrics.kugou.com` | REST | `KuGou.kt` |
| 6 | LrcLib | `https://lrclib.net` | REST | `LrcLib.kt` |
| 7 | Paxsenix/Apple Music | `https://lyrics.paxsenix.org`, `https://amp-api.music.apple.com` | REST | `Paxsenix.kt` |
| 8 | Last.fm | `https://ws.audioscrobbler.com/2.0/` | REST (form POST) | `LastFM.kt` |
| 9 | Shazam | `https://amp.shazam.com/discovery/v5/` | REST (POST JSON) | `Shazam.kt` |
| 10 | Discord OAuth | `https://discord.com/api/v10/oauth2/` | REST | `DiscordAuth.kt` |
| 11 | Discord Gateway | `wss://gateway.discord.gg/?v=10` | WebSocket | `DiscordGateway.kt` |
| 12 | Discord External Assets | `https://discord.com/api/v9/applications/{id}/external-assets` | REST | `DiscordExternalAssets.kt` |
| 13 | GitHub (App Updates) | `https://api.github.com/repos/flex-playerGroup/flex-player` | REST | `Updater.kt` |
| 14 | GitHub (KMP) | `https://api.github.com/repos/flex-playerGroup/flex-player-KMP` | REST | `Updater.kt` |
| 15 | GitHub (AutoEq) | `https://api.github.com/repos/ndellagrotte/AutoEq` | REST | `GitHubAutoEqSearch.kt` |
| 16 | Listen Together | `wss://metroserverx.meowery.eu/ws` | WebSocket (Protobuf) | `ListenTogetherClient.kt` |
| 17 | YouTube BotGuard | `https://www.youtube.com/api/jnn/v1/` | REST | `PoTokenGenerator.kt` |
| 18 | Return YouTube Dislike | `https://returnyoutubedislikeapi.com` | REST | `InnerTube.kt` |
| 19 | LyricsPlus Mirrors | `lyricsplus.binimum.org` + 3 others | REST | `LyricsPlusProvider.kt` |
| 20 | Binimum Lyrics | `https://lyrics-api.binimum.org/` | REST | `LyricsPlusProvider.kt` |
| 21 | Apple Music Token | `https://beta.music.apple.com` | REST | `Paxsenix.kt` |
