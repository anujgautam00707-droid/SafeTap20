# SafeTap Project Guidelines

SafeTap is an Android personal safety application. All development on this repository must adhere to the following rules and standards.

## Technical Stack & Architecture

- **Language:** Kotlin only
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Design Philosophy:** Clean Architecture principles (domain model, data sources, repositories, viewmodels, screens)
- **Authentication:** Firebase Authentication

## Development Principles

- **Incremental Progress:** Always prefer small, incremental changes over large rewrites. Keep code production-ready at all times.
- **Safety & Preservation:**
  - Never rewrite working code unless explicitly requested.
  - Never redesign UI unless explicitly requested.
  - Preserve existing navigation.
  - Preserve package structure.
- **Documentation & Communication:** Explain every file created or modified in detail.

## Build & Verification Workflow

- **After Every Feature:**
  - Run `./gradlew compileDebugKotlin` to verify compilation.
  - Run `./gradlew assembleDebug` to verify the build.
- **Error Resolution:** Fix all compile/build errors before stopping or submitting work.
- **Approval Flow:** Wait for user approval before implementing the next feature.
