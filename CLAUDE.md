# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

- **Build APK**: `./gradlew assembleDebug`
- **Install to device**: `./gradlew installDebug`
- **Build + Install**: `./gradlew assembleDebug installDebug`
- **Auto-build on changes**: `.\watch.ps1` (polls every 1.5s, builds + installs + launches)
- **One-shot build + install**: `.\build-run.bat`
- **Clean build**: `./gradlew clean assembleDebug installDebug`
- **Force recompile**: `./gradlew --rerun-tasks assembleDebug installDebug` 
- **Clear stale class cache**: delete `app/build/tmp/kotlin-classes/` and `app/build/outputs/apk/`
> If changes don't seem to take effect after a normal build, try **force recompile** first before clearing caches.

## Architecture

- **ViewPager2 carousel** with infinite loop (`Int.MAX_VALUE` items via `FragmentStateAdapter`).
- **PageSettings singleton** tracks which pages are enabled (`PageType.CLOCK`, `CALENDAR`, `TODO`, `WIKIPEDIA`). Listener pattern rebuilds the adapter on toggle.
- **PhoneThingApp** `Application` subclass initializes `AppCompatDelegate.setDefaultNightMode()` from shared prefs before any activity starts.
- **viewBinding** used throughout (e.g., `FragmentCalendarBinding`, `ActivityMainBinding`).

## Project Structure
- Main package: `app/src/main/java/com/example/phonething/`
- `MainActivity.kt`, `PhoneThingApp.kt` — app entry points
- `ui/` — `MainPagerAdapter.kt`, `PageSettings.kt`
- `ui/fragments/` — page fragments (`ClockFragment`, `CalendarFragment`, `TodoFragment`, `WikipediaFragment`, `BlankFragment`) plus supporting classes (`CalendarEvent.kt`, `TodoAdapter.kt`)

## Theme System

- Dark/light mode via `values/values-night` resource qualifiers.
- Accent color defined in `colors.xml` (single source of truth for the app's accent).
- `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES/NO)` toggled from settings overlay.
- Background (`#1A1A1A` dark / white light), surface (`#222222`), text colors.

## Pages

### Clock
- 4-element minute flip animation (prev, current, next, new) with 420ms `DecelerateInterpolator`.
- Uses `Handler` + `postDelayed` for minute detection.

### Calendar
- Programmatic 6-week grid: `LinearLayout` of horizontal `LinearLayout` rows, equal height divided by `post()` measurement.
- Day-of-week header row via `GridLayout`.
- Max **2 bullet events per cell** with `"+ N more"` overflow text.
- **No multi-day banner events** — single-day events only.
- Click a day cell → modal dialog listing all events for that day.
- Each event row in dialog has **Edit** and **Del** buttons.
- **"+ Add Event"** at bottom of dialog opens new event prompt.
- Edit/delete/add auto-refresh: rebuilds calendar grid and reopens the day dialog.
- Uses `AlertDialog.Builder` for modals, `EditText` for input.
- Sample events hardcoded in `events` mutable list.

### Todo
- Two `RecyclerView` columns (Doing / Done).
- `ItemTouchHelper` for long-press drag-to-reorder.
- Double-tap on item reveals inline action row (Edit / Delete).
- `DatePickerDialog` for due date selection.
- Sample tasks seeded via `seedSampleTasks()`.

### Wikipedia
- Fetches Today's Featured Article from Wikipedia API on background thread.
- `HttpURLConnection` + JSON parsing + HTML-to-plain-text cleaning.
- Tap to retry on error.
- `Handler` for UI thread updates.

## Key Patterns

- **Programmatic view building**: Fragments build UI in code (no inflation from XML for dynamic parts).
- **`post()` for layout measurements**: Used in calendar to distribute row heights after measure pass.
- **`AlertDialog.Builder`** for all dialogs (no DialogFragment usage).
- **`Handler`** for background-to-UI thread communication (no coroutines/Flow in this project).
- **No navigation component** — single Activity with ViewPager2, settings overlay shown on tap.

## Session Management
When I say "handoff", generate a full continuation prompt I can paste into a new session. Include: current project state, files modified, decisions made, next steps, and any important context. Ask for my approval before finalizing it.

## Git workflow
- For each new feature or logical chunk of work, create a new branch off main (e.g. `feature/short-description`) before starting work.
- For trivial one-line fixes, branching is optional
- Once the feature is complete and working, automatically `git add` and `git commit` the changes on that branch with a clear, descriptive commit message.
- After committing, remind me to review the changes and push the branch (don't push automatically — just remind me).
- Once I confirm the branch has been pushed and reviewed, ask me if I'd like to merge it into main. Don't merge automatically.
- Don't commit directly to main.
- Always confirm the current branch before making changes.