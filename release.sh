#!/bin/bash

# DrawBox Release Script
#
# Releases one publishable module at a time. Prompts for the module and the
# kind of version bump if not passed on the command line; the next version is
# computed from the module's current VERSION_NAME.
#
# Usage: ./release.sh [module] [bump]
#   module : drawbox | drawbox-ui                         (prompted if omitted)
#   bump   : major | minor | patch | alpha | beta | rc |  (prompted if omitted)
#            stable | <explicit-version e.g. 2.2.0>
#
# Bump semantics (current → next):
#   major   2.1.3        → 3.0.0
#   minor   2.1.3        → 2.2.0
#   patch   2.1.3        → 2.1.4
#   alpha   2.1.0-alpha02→ 2.1.0-alpha03   (stable 2.1.0 → 2.2.0-alpha01)
#   beta    2.1.0-alpha02→ 2.1.0-beta01    (stable 2.1.0 → 2.2.0-beta01)
#   rc      2.1.0-beta01 → 2.1.0-rc01      (stable 2.1.0 → 2.2.0-rc01)
#   stable  2.1.0-rc01   → 2.1.0           (drops the pre-release suffix)
#
# Examples:
#   ./release.sh                     # fully interactive
#   ./release.sh drawbox minor
#   ./release.sh drawbox-ui alpha
#   ./release.sh drawbox 2.2.0       # explicit version still accepted

set -e

# Restore the cursor if the script exits mid-prompt (Ctrl-C, error, etc.).
trap 'tput cnorm 2>/dev/null' EXIT

# ── Interactive single-select prompt (Claude-style arrow-key menu) ──────────
# Usage: ask_index "Question" RESULT_VAR "label 0" "label 1" ...
# Sets RESULT_VAR to the zero-based index of the highlighted option. Navigate
# with ↑/↓ (or k/j) and confirm with Enter. Falls back to a numbered text
# prompt when stdin/stdout is not a TTY (piped input, CI).
ask_index() {
    local prompt=$1 __out=$2
    shift 2
    local options=("$@")
    local n=${#options[@]}

    # Non-interactive fallback: numbered list read from stdin.
    if [ ! -t 0 ] || [ ! -t 1 ]; then
        echo "$prompt" >&2
        local i
        for i in "${!options[@]}"; do
            printf "  %d) %s\n" "$((i + 1))" "${options[$i]}" >&2
        done
        local pick
        printf "Enter 1-%d: " "$n" >&2
        read -r pick
        if ! [[ "$pick" =~ ^[0-9]+$ ]] || [ "$pick" -lt 1 ] || [ "$pick" -gt "$n" ]; then
            echo "❌ Invalid selection" >&2
            exit 1
        fi
        printf -v "$__out" '%s' "$((pick - 1))"
        return
    fi

    local selected=0 key i
    printf '%s\n' "$prompt"
    printf '\033[2m  (↑/↓ to move, Enter to select)\033[0m\n'
    tput civis 2>/dev/null
    _ask_render() {
        for i in "${!options[@]}"; do
            if [ "$i" -eq "$selected" ]; then
                printf '\033[36m❯ %s\033[0m\033[K\n' "${options[$i]}"
            else
                printf '  %s\033[K\n' "${options[$i]}"
            fi
        done
    }
    _ask_render
    while true; do
        IFS= read -rsn1 key
        case "$key" in
            $'\x1b')
                # Arrow keys arrive as ESC [ A/B in one burst, so the two
                # trailing bytes are already waiting. Integer timeout only
                # (macOS ships bash 3.2, which rejects fractional -t); guard
                # with `|| true` so a bare-ESC timeout can't trip `set -e`.
                read -rsn2 -t 1 key 2>/dev/null || true
                case "$key" in
                    '[A') selected=$(((selected - 1 + n) % n)) ;;
                    '[B') selected=$(((selected + 1) % n)) ;;
                esac
                ;;
            k) selected=$(((selected - 1 + n) % n)) ;;
            j) selected=$(((selected + 1) % n)) ;;
            '') break ;; # Enter
        esac
        tput cuu "$n" 2>/dev/null
        _ask_render
    done
    tput cnorm 2>/dev/null
    printf -v "$__out" '%s' "$selected"
}

# Usage: ask_yes_no "Question" RESULT_VAR  → sets RESULT_VAR to "y" or "n".
ask_yes_no() {
    local __yn=$2 _idx
    ask_index "$1" _idx "Yes" "No"
    [ "$_idx" -eq 0 ] && printf -v "$__yn" 'y' || printf -v "$__yn" 'n'
}

# ── Module selection ────────────────────────────────────────────────────────
MODULE=$1
if [ -z "$MODULE" ]; then
    ask_index "📦 Which module do you want to release?" _MIDX \
        "drawbox      → io.ak1:drawbox" \
        "drawbox-ui   → io.ak1:drawbox-ui"
    case "$_MIDX" in
        0) MODULE="drawbox" ;;
        1) MODULE="drawbox-ui" ;;
    esac
fi

# Normalize the answer into a canonical module key.
case "$MODULE" in
    1|drawbox|DrawBox)   MODULE="drawbox" ;;
    2|drawbox-ui)        MODULE="drawbox-ui" ;;
    *)
        echo "❌ Unknown module: '$MODULE' (expected 'drawbox' or 'drawbox-ui')"
        exit 1
        ;;
esac

# ── Per-module configuration ────────────────────────────────────────────────
# PROPS_FILE      : gradle.properties holding VERSION_NAME for the module
# GRADLE_PROJECT  : Gradle project path used for the publish command hint
# ARTIFACT        : Maven coordinates (for the verify link)
# TAG_PREFIX      : release tag namespace (kept per-module so versions can't clash)
# BRANCH_PREFIX   : release branch namespace
# UPDATE_DOCS     : whether ./gradlew updateVersion applies (docs only ship core coords)
case "$MODULE" in
    drawbox)
        PROPS_FILE="DrawBox/gradle.properties"
        GRADLE_PROJECT=":DrawBox"
        ARTIFACT="io.ak1:drawbox"
        TAG_PREFIX="v"
        BRANCH_PREFIX="release/"
        UPDATE_DOCS=true
        ;;
    drawbox-ui)
        PROPS_FILE="drawbox-ui/gradle.properties"
        GRADLE_PROJECT=":drawbox-ui"
        ARTIFACT="io.ak1:drawbox-ui"
        TAG_PREFIX="drawbox-ui-v"
        BRANCH_PREFIX="release/drawbox-ui-"
        UPDATE_DOCS=false
        ;;
esac

CURRENT_VERSION=$(grep -E "^VERSION_NAME=" "$PROPS_FILE" | head -1 | cut -d'=' -f2)

# ── Version parsing + bump computation ──────────────────────────────────────
# Split CURRENT_VERSION into BASE (X.Y.Z) and an optional pre-release suffix
# (e.g. alpha02 → stage=alpha, num=02). Pre-release counters are zero-padded and
# that width is preserved when incrementing (alpha02 → alpha03).
BASE=${CURRENT_VERSION%%-*}
PRE=""
[ "$CURRENT_VERSION" != "$BASE" ] && PRE=${CURRENT_VERSION#*-}
MAJOR=${BASE%%.*}
_REST=${BASE#*.}
MINOR=${_REST%%.*}
PATCH=${_REST##*.}
PRE_STAGE=$(printf '%s' "$PRE" | sed -E 's/[0-9]+$//')   # alpha / beta / rc
PRE_NUM=$(printf '%s' "$PRE" | sed -E 's/^[^0-9]*//')     # 02

# Echoes the version that the given bump would produce ("" if not applicable).
bump_version() {
    case "$1" in
        major) echo "$((MAJOR + 1)).0.0" ;;
        minor) echo "$MAJOR.$((MINOR + 1)).0" ;;
        patch) echo "$MAJOR.$MINOR.$((PATCH + 1))" ;;
        alpha|beta|rc)
            if [ -n "$PRE" ]; then
                if [ "$PRE_STAGE" = "$1" ]; then
                    local pnum=${PRE_NUM:-0}
                    local width=${#PRE_NUM}
                    [ "$width" -eq 0 ] && width=2
                    printf "%s-%s%0${width}d\n" "$BASE" "$1" "$((10#$pnum + 1))"
                else
                    # Switching pre-release stage on the same base restarts at 01.
                    echo "$BASE-${1}01"
                fi
            else
                # Opening a pre-release cycle from a stable release bumps the minor.
                echo "$MAJOR.$((MINOR + 1)).0-${1}01"
            fi
            ;;
        stable)
            # Promote: drop the pre-release suffix. No-op if already stable.
            [ -n "$PRE" ] && echo "$BASE" || echo ""
            ;;
    esac
}

# ── Version selection ───────────────────────────────────────────────────────
BUMP=$2
if [ -z "$BUMP" ]; then
    # Build the option list with a live preview of each resulting version.
    # `stable` is only offered when the current version is a pre-release.
    BUMP_KEYS=(major minor patch alpha beta rc)
    BUMP_LABELS=(
        "major   → $(bump_version major)"
        "minor   → $(bump_version minor)"
        "patch   → $(bump_version patch)"
        "alpha   → $(bump_version alpha)"
        "beta    → $(bump_version beta)"
        "rc      → $(bump_version rc)"
    )
    STABLE_PREVIEW=$(bump_version stable)
    if [ -n "$STABLE_PREVIEW" ]; then
        BUMP_KEYS+=(stable)
        BUMP_LABELS+=("stable  → $STABLE_PREVIEW")
    fi
    echo ""
    ask_index "How do you want to bump $MODULE ($CURRENT_VERSION)?" _BIDX "${BUMP_LABELS[@]}"
    BUMP=${BUMP_KEYS[$_BIDX]}
fi

# Resolve BUMP into a concrete VERSION.
case "$BUMP" in
    major|minor|patch|alpha|beta|rc|stable)
        VERSION=$(bump_version "$BUMP")
        if [ -z "$VERSION" ]; then
            echo "❌ '$BUMP' is not applicable to $CURRENT_VERSION (already stable?)"
            exit 1
        fi
        ;;
    [0-9]*)
        VERSION="$BUMP"   # explicit version override
        ;;
    *)
        echo "❌ Unknown bump/version: '$BUMP'"
        exit 1
        ;;
esac

if [ "$VERSION" = "$CURRENT_VERSION" ]; then
    echo "❌ New version ($VERSION) matches the current version"
    exit 1
fi

echo ""
echo "🚀 DrawBox Release"
echo "   Module : $MODULE ($ARTIFACT)"
echo "   Version: $CURRENT_VERSION → $VERSION"
echo "   File   : $PROPS_FILE"
echo ""

# Step 1: Update version in the module's gradle.properties
echo "📝 Step 1: Updating VERSION_NAME in $PROPS_FILE..."
sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" "$PROPS_FILE"
echo "✅ Set VERSION_NAME=$VERSION"
echo ""

# Step 2: Auto-update documentation (core module only — docs reference io.ak1:drawbox)
if [ "$UPDATE_DOCS" = true ]; then
    echo "📝 Step 2: Updating documentation files..."
    ./gradlew updateVersion 2>&1 | grep -E "📝|✅|⏭️|✨|Updated"
    echo ""
else
    echo "⏭️  Step 2: Skipping doc update ($MODULE has no coordinates in README/docs)"
    echo ""
fi

# Step 3: Show changes
echo "📋 Step 3: Changes made:"
git diff --stat
echo ""

# Helper: restore any files this script may have touched (used on cancel).
restore_changes() {
    git checkout "$PROPS_FILE" 2>/dev/null || true
    if [ "$UPDATE_DOCS" = true ]; then
        git checkout README.md docs/ 2>/dev/null || true
    fi
}

# Step 3a: Refuse to release with stray untracked files under src/.
# Either commit them, delete them, or add them to .gitignore — but do not
# ship a release with uncommitted source sitting in the working tree.
UNTRACKED_SRC=$(git ls-files --others --exclude-standard -- '*/src/*' 'src/*' 2>/dev/null || true)
if [ -n "$UNTRACKED_SRC" ]; then
    echo "⚠️  Untracked files detected under src/:"
    echo ""
    echo "$UNTRACKED_SRC" | sed 's/^/   - /'
    echo ""
    echo "   Commit them, delete them, or add them to .gitignore before releasing."
    ask_yes_no "   Continue anyway?" SRC_OK
    if [ "$SRC_OK" != "y" ]; then
        echo "❌ Release cancelled"
        restore_changes
        exit 1
    fi
fi

# Step 4: Ask for confirmation
ask_yes_no "🔍 Review the changes above. Continue?" CONFIRM

if [ "$CONFIRM" != "y" ]; then
    echo "❌ Release cancelled"
    restore_changes
    exit 1
fi

RELEASE_BRANCH="${BRANCH_PREFIX}${VERSION}"
TAG="${TAG_PREFIX}${VERSION}"
ARTIFACT_PATH=$(echo "$ARTIFACT" | tr ':' '/')

# Step 5: Move onto the release branch BEFORE committing, so the release commit
# is never born on the branch you happened to be standing on (e.g. main). The
# working-tree edits carry over on checkout.
echo ""
echo "🌿 Step 4: Preparing release branch $RELEASE_BRANCH..."
if [ "$(git rev-parse --abbrev-ref HEAD)" = "$RELEASE_BRANCH" ]; then
    echo "✅ Already on $RELEASE_BRANCH"
elif git show-ref --verify --quiet "refs/heads/$RELEASE_BRANCH"; then
    git checkout "$RELEASE_BRANCH"
    echo "✅ Switched to existing $RELEASE_BRANCH"
else
    git checkout -b "$RELEASE_BRANCH"
    echo "✅ Created $RELEASE_BRANCH"
fi
echo ""

# Step 6: Commit — stage only the files this release touched, never `git add .`,
# so unrelated working-tree changes don't get swept into the release commit.
echo "✅ Step 5: Committing changes..."
git add "$PROPS_FILE"
if [ "$UPDATE_DOCS" = true ]; then
    git add README.md docs/ gradle/libs.versions.toml
fi
git commit -m "chore: release $MODULE version $VERSION"
echo "✅ Committed: chore: release $MODULE version $VERSION"
echo ""

# Step 7: Tag the release commit
echo "🏷️  Step 6: Tagging release as $TAG..."
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
    echo "⚠️  Tag $TAG already exists locally — skipping. Delete it first with"
    echo "   'git tag -d $TAG' (and 'git push origin :refs/tags/$TAG' if pushed)"
    echo "   to retag."
else
    git tag -a "$TAG" -m "Release $MODULE $VERSION"
    echo "✅ Tagged $TAG → $(git rev-parse --short HEAD)"
fi
echo ""

# Step 8: Gated push + PR. Pushing and opening a PR are outward-facing, so they
# get their own confirmation; declining (or missing gh) falls back to printing
# the manual commands.
print_manual_steps() {
    echo "ℹ️  Manual steps to finish the release:"
    echo "     git push -u origin $RELEASE_BRANCH"
    echo "     git push origin $TAG"
    echo "   Open a PR: https://github.com/akshay2211/DrawBox/compare/main...$RELEASE_BRANCH"
}

ask_yes_no "🚀 Push $RELEASE_BRANCH + $TAG and open a PR to main?" DO_PUSH
echo ""
if [ "$DO_PUSH" = "y" ]; then
    git push -u origin "$RELEASE_BRANCH"
    git push origin "$TAG"
    echo "✅ Pushed branch + tag"
    if command -v gh >/dev/null 2>&1; then
        gh pr create --base main --head "$RELEASE_BRANCH" \
            --title "chore: release $MODULE $VERSION" \
            --body "Release \`$MODULE\` **$VERSION** (\`$ARTIFACT\`).

Publish to Maven Central after merge (needs Sonatype creds + signing key):
\`\`\`
./gradlew $GRADLE_PROJECT:publishAndReleaseToMavenCentral
\`\`\`"
        echo "✅ PR opened"
    else
        echo "⚠️  gh CLI not found — open the PR manually:"
        echo "   https://github.com/akshay2211/DrawBox/compare/main...$RELEASE_BRANCH"
    fi
else
    print_manual_steps
fi
echo ""

# Step 9: Post-merge / publish reminders
echo "🎉 Release prepared! Remaining steps:"
echo ""
echo "1. Merge the PR into main when CI is green."
echo "2. Publish to Maven Central (needs Sonatype creds + signing key):"
echo "     ./gradlew $GRADLE_PROJECT:publishAndReleaseToMavenCentral"
echo "3. Watch deployment:    https://github.com/akshay2211/DrawBox/actions"
echo "4. Check Maven Central: https://central.sonatype.com/artifact/$ARTIFACT_PATH"
echo "5. Verify docs:         https://akshay2211.github.io/DrawBox/"
echo ""