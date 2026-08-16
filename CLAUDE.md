# Project Guidelines & Instructions

## Git Commit Guidelines

### Format
Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <concise description>

[optional body: direct and factual bullet points of changes]
```

### Allowed Types
- `feat:` A new feature
- `fix:` A bug fix
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `style:` Changes that do not affect the meaning of the code (white-space, formatting, etc.)
- `perf:` A code change that improves performance
- `test:` Adding missing tests or correcting existing tests
- `chore:` Changes to the build process, tooling, dependencies, or auxiliary tools
- `ci:` Changes to CI configuration files and scripts (e.g. GitHub Actions)
- `docs:` Documentation only changes

### Rules for Commit Messages
- **Always use standard commit prefixes** (`feat:`, `fix:`, `refactor:`, etc.) — never omit them.
- **Direct & concise subject lines**: Use imperative mood (e.g. `refactor(ui): consolidate modal dialogs into ModalView`).
- **NO AI fluff, storytelling, or rambling meta-commentary**:
  - Do NOT write essays explaining thought processes, conversational filler, or self-absorbed post-mortems (e.g. "ebd1e35a read the ask wrong...").
  - Do NOT write narrative stories about what prompt or ask was misinterpreted.
- **Keep descriptions factual, short, and technical**: State what is added, removed, or changed.

### Examples

#### ❌ BAD (Do NOT do this):
```
Put every modal on the "?" popup's own component

ebd1e35a read the ask wrong. It kept MaterialAlertDialogBuilder and restyled
@style/AlertDialog to resemble the tip popup, down to a tween interpolator
standing in for that popup's spring. The result was two modal implementations
agreeing by value - two scrims, two surface colours, two entrance curves -
where one was wanted.

TipOverlayView is now ModalView and is the only modal in the app...
[...followed by 4 more paragraphs of verbose rambling AI essay...]
```

#### ✅ GOOD:
```
refactor(ui): replace MaterialAlertDialogBuilder with unified ModalView

- Rename TipOverlayView to ModalView and consolidate modal implementations
- Replace MaterialAlertDialogBuilder dialogs across all fragments
- Add secondary button support and custom content slots to ModalView
- Handle dismiss on outside scrim tap and hardware back button
```

---

## Development & Build Commands

- **Run ktlint**: `./ktlint`
- **Run Unit Tests**: `./gradlew test` (or specific module: `./gradlew :app:testDebugUnitTest`)
- **Build Release APK**: `./gradlew :app:assembleRelease`
- **Build Debug APK**: `./gradlew :app:assembleDebug`
