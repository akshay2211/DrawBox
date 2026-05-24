# iOS app

The Swift files in this folder are scaffolding for the iOS host app. The Xcode project (`iosApp.xcodeproj`) is not committed because Xcode-generated `project.pbxproj` files are not portable hand-authored artifacts — create one in Xcode.

(The "No such module 'Shared' / 'UIKit'" SourceKit diagnostics here are expected until the Xcode project is created and built.)

## Steps

1. Open Xcode → File → New → Project → iOS → App.
2. Save it into this `iosApp/` directory so the structure becomes `iosApp/iosApp.xcodeproj` + `iosApp/iosApp/iOSApp.swift`, etc. Replace the auto-generated `iOSApp.swift`, `ContentView.swift`, and `Info.plist` with the files already provided here.
3. In the project's **Build Phases → Run Script (before Compile Sources)**, add:

   ```sh
   cd "$SRCROOT/.."
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```

4. Add to **Build Settings → Framework Search Paths**:

   ```
   $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
   ```

5. Add `Shared.framework` to **Frameworks, Libraries, and Embedded Content** (Embed & Sign).
6. Build & run on a simulator or device.

The framework is produced by the `:shared` Kotlin Multiplatform module and exports `MainViewController()` (referenced as `MainViewControllerKt.MainViewController()` from Swift).
