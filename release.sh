#!/bin/bash

# This script circumvents the absence of a distribution repository for Play SQL. It stores releases
# in ../releases

set -e -u
if [ "$#" -ne 2 ]; then
    echo
    echo "Please provide the version number. History:"
    # Checked-in versions:
    git tag -l | sed  's/[a-z-]*\([0-9][a-z0-9.-]*\)/Released: \1/' | sort -u
    # Current version:
    if [ -f pom.xml ] ; then
        echo -n "Current: " ; xpath pom.xml "project/version/text()" 2>/dev/null
        echo # because xpath doesn't end with \n
    fi
    echo
    echo "Usage ./release.sh 1.1.1 1.1.2-SNAPSHOT"
    echo
    exit 1
fi
if [ ! -d ../releases ] ; then
    echo
    echo "The directory ../releases is required"
    echo
    exit 1
fi

VER="$1"
NEXT_VER="$2"
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
CURRENT_COMMIT=$(git rev-parse HEAD)
LAST_COMMIT=$(git log -1 --pretty=oneline)
HAS_MVN=$(echo $LAST_COMMIT | grep "[auto] Set version to" || true)
HAS_NO_RELEASE=$(grep "NO_RELEASE" -l -RI * | grep -v "/target/" | wc -l)

if [ -d ../target-psea] ; then
    echo "Please remove the directory ../target-psea before releasing."
    exit 2
fi

echo
echo
echo "Going to release $VER and reset to $NEXT_VER"
echo "Current branch is $CURRENT_BRANCH and current commit $CURRENT_COMMIT"
echo "Has mvn: $HAS_MVN"
echo

if [ $HAS_NO_RELEASE -ne 1 ] ; then
    echo "There is a comment somewhere with the tag NO_RELEASE. Please check."
    grep "NO_RELEASE" -l -RI *
fi

if [ -n "$HAS_MVN" ] ; then
    git log HEAD^^^..HEAD --format=oneline
    echo
    echo "Will reset to previous commit (and remove the tag psea-parent-$VER if exists)"
    read -p "Press ENTER"
    git reset --hard HEAD^

    LAST_COMMIT=$(git log -1 --pretty=oneline)
    HAS_MVN=$(echo $LAST_COMMIT | grep "maven-release-plugin" || true)
    if [ -n "$HAS_MVN" ] ; then
        git reset --hard HEAD^
    fi

    git tag -d psea-$VER || true
    git tag -d psea-parent-$VER || true

    echo "Last rev: "
    git log -1 --format=oneline
    echo
    git push --force
    echo
fi

echo "Press any key to continue"
read


if [ -d target/confluence/home ] ; then
    echo "Moving ./target to ../target-psea"
    mv ./target ../target-psea
fi

echo
echo
echo "Changing version to $VER"
echo
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$VER
mvn clean install
git commit -am "[auto] Set version to $VER"
git tag -a psea-parent-$VER -m "Release $VER"

# We don't move the other artifacts to ../releases, since the local maven repo is enough to publish them
ls target/*.jar
cp target/*.jar ../releases/
read


echo
echo
echo "Changing version to $NEXT_VER"
echo
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$NEXT_VER
mvn clean install
git commit -am "[auto] Set version to $NEXT_VER"

echo
echo
echo "Pushing..."
echo
git push --tags

echo
echo

if [ -d ../target-psea ] ; then
    echo "Restoring ../target-psea ./target"
    rm -rf ./target
    mv ../target-psea ./target
    rm ./target/*.jar
fi

echo "Get cracking, now"
echo
