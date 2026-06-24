#!/bin/bash

# DrawBox Release Script
# Usage: ./release.sh 2.1.0

set -e

if [ -z "$1" ]; then
    echo "❌ Usage: ./release.sh <version>"
    echo "Example: ./release.sh 2.1.0"
    exit 1
fi

VERSION=$1
echo "🚀 DrawBox Release: $VERSION"
echo ""

# Step 1: Update version in gradle.properties
echo "📝 Step 1: Updating version in DrawBox/gradle.properties..."
sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$VERSION/" DrawBox/gradle.properties
echo "✅ Set VERSION_NAME=$VERSION"
echo ""

# Step 2: Auto-update all documentation files
echo "📝 Step 2: Updating all documentation files..."
./gradlew updateVersion 2>&1 | grep -E "📝|✅|⏭️|✨|Updated"
echo ""

# Step 3: Show changes
echo "📋 Step 3: Changes made:"
git diff --stat
echo ""

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
    echo "   Continue anyway? (y/n)"
    read -r SRC_OK
    if [ "$SRC_OK" != "y" ]; then
        echo "❌ Release cancelled"
        git checkout DrawBox/gradle.properties README.md docs/
        exit 1
    fi
fi

# Step 4: Ask for confirmation
echo "🔍 Review the changes above. Continue? (y/n)"
read -r CONFIRM

if [ "$CONFIRM" != "y" ]; then
    echo "❌ Release cancelled"
    git checkout DrawBox/gradle.properties README.md docs/
    exit 1
fi

# Step 5: Commit
echo ""
echo "✅ Step 4: Committing changes..."
git add .
git commit -m "chore: release version $VERSION"
echo "✅ Committed: chore: release version $VERSION"
echo ""

# Step 6: Tag the release commit
TAG="v$VERSION"
echo "🏷️  Step 5: Tagging release as $TAG..."
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
    echo "⚠️  Tag $TAG already exists locally — skipping. Delete it first with"
    echo "   'git tag -d $TAG' (and 'git push origin :refs/tags/$TAG' if pushed)"
    echo "   to retag."
else
    git tag -a "$TAG" -m "Release $VERSION"
    echo "✅ Tagged $TAG → $(git rev-parse --short HEAD)"
fi
echo ""

# Step 7: Show next steps
RELEASE_BRANCH="release/$VERSION"
echo "🎉 Release prepared! Next steps:"
echo ""
echo "1. Review commit + tag: git log -1 && git show $TAG --stat"
echo "2. Push to release branch:"
echo "     git push origin HEAD:refs/heads/$RELEASE_BRANCH"
echo "     git push origin $TAG"
echo "   Open a PR from $RELEASE_BRANCH → main when CI is green."
echo "3. Watch deployment:    https://github.com/akshay2211/DrawBox/actions"
echo "4. Check Maven Central: https://search.maven.org/artifact/io.ak1/drawbox"
echo "5. Verify docs:         https://akshay2211.github.io/DrawBox/"
echo ""