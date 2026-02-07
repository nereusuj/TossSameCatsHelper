# Project Context for Agents: Toss Same Cats Helper

## Project Overview
This is an Android application designed to assist with the "Toss Same Cats" game. It uses screen capture to analyze the game board, identifies matching cards using image processing, and automatically plays the game using an Accessibility Service.

## Key Architectures & Components

### 1. Core Services
-   **`OverlayService.kt`**: The central controller.
    -   **Role**: Manages the floating UI (Overlay), coordinates screen capture, image analysis, and auto-play execution.
    -   **State Management**: Handles the analysis/play loop, including the 8-time repetition logic.
    -   **UI**: Manages `controlsView` (Start/Stop buttons) and `resultView` (Overlay drawing).
-   **`AutoClickService.kt`**: An AccessibilityService.
    -   **Role**: Performs physical clicks on the screen based on coordinates provided by `OverlayService`.
    -   **Pattern**: Singleton instance access (`instance`) is used by `OverlayService` to dispatch gestures.

### 2. Image Processing
-   **`ImageAnalyzer.kt`**:
    -   **Role**: Analyzes the bitmap captured from the screen.
    -   **Algorithm**:
        1.  **Flood Fill**: Separates background/foreground.
        2.  **Blob Detection**: Identifies card candidates.
        3.  **MSE (Mean Squared Error)**: Compares card images to find matches.
    -   **Input**: `Bitmap` (Screenshot).
    -   **Output**: List of `CardResult` (Coordinates and Group IDs).

### 3. Screen Capture
-   **`ScreenCaptureManager.kt`**:
    -   **Role**: Uses `MediaProjection` API to capture the screen content as a `Bitmap`.

## Critical Logic Implementation Details

### Auto-Play Loop (`OverlayService`)
The auto-play feature is implemented as a recursive-like coroutine loop in `OverlayService`:
1.  **Start**: User clicks "Start".
2.  **Analysis**: `startAnalysis()` captures screen -> `ImageAnalyzer` analyzes -> Results shown.
3.  **Action**: `startAutoPlay()` waits 4s -> Iterates through results -> Calls `AutoClickService.click`.
4.  **Repetition**:
    -   After clicks finish, it waits 3.5s.
    -   Clears the overlay.
    -   Increments counter.
    -   Recursively calls `startAnalysis()` until `MAX_REPETITIONS` (8) is reached.

### Coordinate System
-   The app draws over the screen using `WindowManager`.
-   Coordinates from `ImageAnalyzer` must match the screen coordinates for `AutoClickService` to work correctly.
-   Offset randomization is applied in `clickCard` to mimic human behavior.

## Build Information
-   **Min SDK**: 31 (Android 12)
-   **Target SDK**: 36
-   **Language**: Kotlin
-   **Permissions**: Overlay (SYSTEM_ALERT_WINDOW), Accessibility, Media Projection.

## Future Considerations
-   If the game UI changes, `ImageAnalyzer` parameters (thresholds, blob sizes) might need adjustment.
-   `AutoClickService` relies on the user enabling Accessibility permissions manually.
