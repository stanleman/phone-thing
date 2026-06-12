# Phone Thing

Turn your old phone into a smart display dashboard. Inspired by the TRMNL e-ink aesthetic.

## Features

- **Landscape-only orientation** — perfect for docked phones
- **Swipe navigation** — horizontal swipe between pages
- **Big clock page** — giant time display with live seconds
- **Calendar page** — monthly calendar with today highlighted
- **To-Do page** — two-column Doing/Done list with tap-to-toggle and add button
- **Minimal dark UI** — TRMNL-inspired e-ink look

## Development

### VS Code Workflow (recommended)

You have two options:

**A) Auto-watch mode (hot-reload-like)** — keeps running, rebuilds on every save:

```powershell
# In VS Code terminal:
.\watch.ps1
```

Then just edit and save any `.kt`, `.xml`, or `.gradle` file — it auto-builds, installs, and launches on your device. Leave it running in the background.

**B) One-shot build + run:**

```powershell
.\build-run.bat
```

### Android Studio

1. Open this folder in Android Studio
2. Let Gradle sync complete  
3. Hit Run (▶) or Build → Build Bundle(s) / APK(s)

### Manual CLI (if you prefer)

```bash
# Build
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell monkey -p com.example.phonething 1

# All in one line:
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell monkey -p com.example.phonething 1
```

## Project Structure

```
app/src/main/
├── java/com/example/phonething/
│   ├── MainActivity.kt           # Main activity with ViewPager2
│   ├── ui/
│   │   ├── MainPagerAdapter.kt   # Fragment adapter for swiping
│   │   └── fragments/
│   │       ├── ClockFragment.kt  # Big time display page
│   │       └── CalendarFragment.kt # Monthly calendar page
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_clock.xml
│   │   ├── fragment_calendar.xml
│   │   ├── fragment_todo.xml
│   │   └── item_task.xml
│   ├── values/
│   │   ├── colors.xml
│   │   ├── strings.xml
│   │   └── themes.xml
│   └── drawable/ ...
```

## Adding More Pages

1. Create a new fragment class in `ui/fragments/`
2. Create its layout XML in `res/layout/`
3. Add it to `MainPagerAdapter.kt`
4. Add a dot to `activity_main.xml`
