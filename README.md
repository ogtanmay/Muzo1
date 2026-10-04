# Muzo (Android Native)

A modern, high-performance Android music streaming client rewritten in **Kotlin** and **Jetpack Compose**, inspired by YouTube Music with real-time synchronized lyrics and liquid aesthetic UI.

---

## Features

- **Home Feed**:
  - Trending Songs & Music Videos
  - Top on Muzo chart
  - Featured & Curated Playlists
- **Search & Discovery**:
  - Real-time search suggestions
  - Filters for Songs, Videos, Playlists, Albums
  - Instant stream resolution
- **Immersive Full Player**:
  - High-resolution album artwork
  - **Synchronized Lyrics**: Auto-scrolling, line-by-line lyrics powered by Atomix and LRCLIB APIs, with click-to-seek support
  - Scrubber progress slider with minute-second tracking
  - Audio playback controls: Shuffle, Repeat (Off / All / One), Play / Pause, Previous / Next
  - Up-Next Queue sheet
  - Sleep Timer with customizable countdown
- **Library & Custom Playlists**:
  - Liked Songs & Favorites
  - Listening History
  - Custom Playlist creator and manager
  - **Spotify Playlist Importer**: Convert public Spotify playlist URLs directly into Muzo
- **Community Feed**:
  - Stream community-uploaded and creator-shared music
- **Settings & Customization**:
  - Appearance: AMOLED (Pure Black), Dark, Light, System Default
  - Streaming Audio Quality: High (320 kbps), Standard (160 kbps), Data Saver (96 kbps)
  - Clear listening history and cache

---

## Architecture & Tech Stack

- **UI Framework**: Android Jetpack Compose with Material Design 3
- **Audio Engine**: AndroidX Media3 ExoPlayer with foreground `MediaSessionService`
- **Networking**: OkHttp 4 with Coroutines & StateFlow
- **Stream Extraction**: Saavn high-bitrate stream resolver + YouTube InnerTube VR player endpoint fallback
- **Lyrics Engine**: Multi-tiered lyrics resolver (Atomix & LRCLIB) with synced LRC parser
- **Image Loading**: Coil Compose
- **Data Persistence**: SharedPreferences local store with JSON serialization
