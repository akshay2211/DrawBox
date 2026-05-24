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

# Step 6: Show next steps
echo "🎉 Release prepared! Next steps:"
echo ""
echo "1. Review commit: git log -1"
echo "2. Push to main:  git push origin main"
echo "3. Watch deployment: https://github.com/akshay2211/DrawBox/actions"
echo "4. Check Maven Central: https://search.maven.org/artifact/io.ak1/drawbox"
echo "5. Verify docs: https://akshay2211.github.io/DrawBox/"
echo ""