# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Project Overview

RodApp is a native Android app (Kotlin) for motorcycle owners to manage fuel logs, maintenance records, and legal documents (SOAT, RTM, Licencia). It uses a **Single Activity + Navigation Component** architecture with Material Design 3.

- **compileSdk:** 36 | **minSdk:** 30 | **targetSdk:** 35
- **Language:** Kotlin, Java 11 compatibility
- **Build:** Gradle Kotlin DSL (`.kts`) with version catalog at `gradle/libs.versions.toml`

## Architecture

### Activity & Navigation Flow

```
firstActivity (splash, 2.5s) → Start_activity (welcome)
    ↓
login / register_user (auth activities)
    ↓
MainActivity (Single Activity hub) ← all main fragments live here
AdminActivity (separate admin panel)
```

**MainActivity** hosts a `NavHostFragment` connected to `res/navigation/nav_graph.xml`. It combines a `DrawerLayout` (side nav) with a `BottomNavigationView` (5 main tabs).

Auth activities navigate to `MainActivity` using `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` to clear the back stack.

### Fragment Structure

All fragments live in `app/src/main/java/com/example/rodapp/fragments/`.

**Bottom nav destinations** (top-level, no back arrow):
- `InicioFragment` — dashboard with FAB menu for quick fuel/maintenance entry
- `GarajeFragment` — empty state; navigates to `RegistroMotoFragment` to add a bike, then `GarajeDocumentosFragment`
- `MapaFragment` — OSMDroid map
- `HistorialFragment` — activity history
- `PerfilFragment` — user settings and logout

**Document registration flow** (reachable from GarajeDocumentosFragment):
- `RegistroSOATFragment` → SOAT insurance form
- `RegistroRTMFragment` → Technical inspection form
- `DocumentosAdicionalesFragment` → Licencia + Seguro Todo Riesgo + custom docs
- `NuevoDocumentoFragment` → Custom document creation
- `DocumentoDetalleFragment` → Document detail view

### ViewBinding

ViewBinding is enabled globally. All fragments use the standard nullable-binding pattern:

```kotlin
private var _binding: FragmentXxxBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentXxxBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

Navigation between fragments uses `findNavController().navigate(R.id.navigation_xxx)`.

### Design System

All colors are defined in `res/values/colors.xml`. Never hardcode hex values.

| Role | Name | Value |
|---|---|---|
| Background | `background_blue` | `#0A1619` |
| Card surface | `dark_blue` | `#132328` |
| Card surface alt | `surface_card` | `#192F33` |
| Primary action | `button_blue` / `cyan_primary` | `#11B4D4` |
| Hint/subtitle text | `gris_hint` | `#94A3B8` |
| White text | `white` | `#F1F5F9` |
| Maintenance accent | `orange_secondary` | `#FF8C00` |
| Alert | `alert_red` | `#BA1A1A` |

Theme: `Theme.RodApp` extends `Theme.Material3.DayNight.NoActionBar`. Use `MaterialToolbar` for all top bars.

### String Resources

All UI text is in Spanish (Colombia). Every string must be defined in `res/values/strings.xml` — never use inline string literals in layouts or Kotlin code. When adding UI text, add the `<string>` entry first, then reference it as `@string/name`.

### Dependencies

- **Navigation:** `androidx.navigation:navigation-fragment-ktx` + `navigation-ui-ktx`
- **Map:** `org.osmdroid:osmdroid-android:6.1.18`
- **UI:** Material 3 (`com.google.android.material`), ConstraintLayout, NestedScrollView
- **Preferences:** `androidx.preference:preference-ktx`

There is currently **no backend/database layer** — no Room, Retrofit, or Supabase dependencies are present in the build yet.

### External Reference Guidelines
**NOTICE** The file **BASE_PROJECT_CONTEXT.md** serves as a supplementary technical reference and pattern library from a parallel academic project. It should be treated as a "blueprint of possibilities" for implementing infrastructure 
like Supabase, Biometrics, Security managers, structure, name of variables, code practices, etc. However, this CLAUDE.md file remains the primary source of truth for the current project’s architecture, navigation flow, and dependencies. Do not perform migrations or apply configurations from the base project unless explicitly instructed to port a specific feature, as doing so may conflict with the current system's established logic.