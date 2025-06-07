# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Go board game implementation in Kotlin with a multi-module architecture:
- **Core module**: Game engine with immutable state management, Zobrist hashing for ko rule detection, and extensible agent system
- **App module**: Android UI using Jetpack Compose (planned/in development)
- Console-based ASCII board rendering for desktop gameplay

## Commands

### Build and Run
```bash
# Build the entire project (core + app modules)
./gradlew build

# Run the core console game (two random bots playing on 9x9 board)
./gradlew :core:run

# Run with assertions enabled (for debugging)
./gradlew :core:run -ea

# Build only the core module
./gradlew :core:build

# Build only the Android app
./gradlew :app:build
```

### Testing
```bash
# Run all tests (core module only)
./gradlew test

# Run tests with more output
./gradlew test --info

# Run a specific test class
./gradlew test --tests "*.BoardTest"

# Run a specific test method
./gradlew test --tests "*.BoardTest.testCapture"

# Run core module tests specifically
./gradlew :core:test

# Run Android instrumented tests
./gradlew :app:connectedAndroidTest
```

### Development
```bash
# Clean build artifacts
./gradlew clean

# Generate IntelliJ IDEA project files
./gradlew idea
```

## Architecture

### Module Structure
- **`:core`** - Pure Kotlin game engine, runnable on JVM (console gameplay)
- **`:app`** - Android application with Jetpack Compose UI, depends on `:core`

### Core Game Engine
The game engine uses immutable data structures with functional transformations:

1. **`Board`** - Manages stone placement, capture detection, and liberty counting
   - Uses a HashMap for stone positions
   - Tracks GoStrings (connected stone groups) efficiently
   - Provides deep copy for move simulation

2. **`GameState`** - Enforces game rules and tracks history
   - Validates moves (no suicide, ko rule)
   - Maintains previous board states for ko detection
   - Uses Zobrist hashing for efficient state comparison

3. **`Move`** - Sealed class hierarchy for type-safe move handling
   - `Play(point)` - Place a stone
   - `Pass` - Pass turn
   - `Resign` - Resign game

### Key Implementation Details

- **Zobrist Hashing**: Pre-generated hash table in `generate_hash.kt` supports boards up to 20x20
- **Ko Rule**: Prevents immediate board position repetition using previous state tracking
- **Liberty Tracking**: GoString class maintains liberties set for O(1) capture detection
- **Coordinate System**: 1-based indexing (row 1 = top, col 1 = left)

### Agent System
Agents implement the `Agent` interface:
- `selectMove(gameState: GameState): Move` - Core decision method
- Current implementation: `RandomBot` with basic eye detection

### Testing Approach
Tests focus on game mechanics correctness:
- Stone placement and merging
- Capture detection
- Liberty counting  
- Ko rule enforcement
- Self-capture prevention