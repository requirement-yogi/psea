#!/bin/bash

set -u
set -e
GREEN_CHECKMARK="[\033[32m✔︎\033[0m]"
RED_MARK="[\033[31m✘\033[0m]"
WARN_MARK="\033[33m/!\\\\\033[0m"

echo
echo "This script is incredible:"
echo "- It reapplies, on the current branch, the changes for Confluence 10,"
echo "- It performs the release,"
echo "- It pushes it as the correct branch."
echo
echo "Good luck debugging it."
echo
echo "Usage: $0 version-to-release"
echo "Example: $0 4.4.2-c10"
echo

VER="$1"
NEXT_VER="$(xpath -e "project/version/text()" pom.xml 2>/dev/null)"
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
NEW_BRANCH="$CURRENT_BRANCH-for-$VER"

echo "VER=\"$VER\""
echo "NEXT_VER=\"$NEXT_VER\""
echo "CURRENT_BRANCH=\"$CURRENT_BRANCH\""
echo "NEW_BRANCH=\"$NEW_BRANCH\""
echo "Branching as: $NEW_BRANCH"
read -p "Ok?"
function suggestToRecover() {
    read -p "Something failed with error code $?. Can you please check?" || exit 78
}

git checkout -b "$NEW_BRANCH" || suggestToRecover

# Apply the "RY-1704 Compatibility with Confluence 10 and Jira 11" commit
git cherry-pick 260f07e0250 || suggestToRecover
./src/update-to-confluence-10.sh || suggestToRecover
./release.sh "$VER" "$NEXT_VER" || suggestToRecover

echo
echo "$GREEN_CHECKMARK Success"
echo


