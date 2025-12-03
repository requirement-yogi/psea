#!/bin/bash

# This script shows warnings about common errors,
# and is used by release.sh before doing a release.

set -e -u
GREEN_CHECKMARK="[\033[32m✔︎\033[0m]"
RED_MARK="[\033[31m✘\033[0m]"
WARN_MARK="\033[33m/!\\\\\033[0m"

echo
echo "This script upgrades the current codebase for Confluence 10"
echo "It is tightly-coupled with the Requirement Yogi codebase"
echo
echo "Usage:"
echo "     - Apply the branch"
echo "     - ./src/update-to-confluence-10.sh"
echo

# find . -name "*.java" -exec sed -i '' -E 's/regex/replacement/' {} +

find . -name "pom.xml" -exec sed -i '' -E 's/org.codehaus.jackson.annotate.*//' {} +
find . -name "*.java" -exec sed -i '' -E 's/\@org.codehaus.jackson.annotate.[A-Za-z\(\) =]*//' {} +

find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/com\.atlassian\.xwork/com.atlassian.struts.xsrf/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/javax\.inject/jakarta.inject/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/<!-- NO_CHANGE -->jakarta.inject/javax.inject/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/javax\.servlet/jakarta.servlet/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/javax\.ws\.rs/jakarta.ws.rs/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/javax\.xml\.bind\.annotation/jakarta.xml.bind.annotation/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/net\.jcip\.annotations/javax.annotation.concurrent/' {} +
find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/javax\.validation\.constraints/jakarta.validation.constraints/' {} +
#find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/jakarta\.ws\.rs\.WebApplicationException/javax.ws.rs.WebApplicationException/' {} +
#find . '(' -name "*.java" -o -name "pom.xml" ')' -exec sed -i '' -E 's/jakarta\.ws\.rs\.core\.Response/javax.ws.rs.core.Response/' {} +

#git commit -am "PSEA-33 Just apply $0"
echo -e "$GREEN_CHECKMARK Go 2027"
