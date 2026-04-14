#!/usr/bin/env bash
set -euo pipefail
WORKDIR=$(pwd)
REPORT_DIR="$WORKDIR/.issues"
mkdir -p "$REPORT_DIR"
TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
REPORT_FILE="$REPORT_DIR/issue_$TIMESTAMP.md"
REMOTE_URL=$(git remote get-url origin 2>/dev/null || true)

echo "# Automated review report — $TIMESTAMP" > "$REPORT_FILE"
echo "Repository: ${REMOTE_URL:-local}" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

FOUND=0

# 1) Quick grep checks: TODOs/FIXMEs
echo "## Quick code smells" >> "$REPORT_FILE"
if grep -R --line-number -E "TODO|FIXME" --exclude-dir=.git --exclude-dir=.venv . | sed -n '1,200p' >> "$REPORT_FILE"; then
    FOUND=1
else
    echo "No TODO/FIXME markers found." >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"

# 2) Detect project type and run light checks if possible
if [ -f pom.xml ]; then
  echo "## Maven project checks" >> "$REPORT_FILE"
  if command -v mvn >/dev/null 2>&1; then
    echo "Running: mvn -DskipTests=true -q package (quick)" >> "$REPORT_FILE"
    if mvn -DskipTests=true -q package 2>> /tmp/ci_review_mvn_err.log; then
      echo "Maven package: success" >> "$REPORT_FILE"
    else
      echo "Maven package: failed (see logs)" >> "$REPORT_FILE"
      sed -n '1,200p' /tmp/ci_review_mvn_err.log >> "$REPORT_FILE"
      FOUND=1
    fi
    # Try spotbugs/checkstyle if configured
    if mvn help:effective-pom | grep -i spotbugs >/dev/null 2>&1; then
      echo "Running spotbugs (if available)" >> "$REPORT_FILE"
      mvn -q spotbugs:check 2>> /tmp/ci_review_spot_err.log || true
      sed -n '1,200p' /tmp/ci_review_spot_err.log >> "$REPORT_FILE" || true
    fi
  else
    echo "Maven not installed; skipping deep checks." >> "$REPORT_FILE"
  fi
elif ls *.gradle* >/dev/null 2>&1; then
  echo "## Gradle project checks" >> "$REPORT_FILE"
  if command -v gradle >/dev/null 2>&1 || command -v ./gradlew >/dev/null 2>&1; then
    echo "Running: gradle check (quick)" >> "$REPORT_FILE"
    if command -v gradle >/dev/null 2>&1; then
      gradle check --no-daemon --console=plain >> /tmp/ci_review_gradle.log 2>&1 || true
    else
      ./gradlew check --console=plain >> /tmp/ci_review_gradle.log 2>&1 || true
    fi
    sed -n '1,200p' /tmp/ci_review_gradle.log >> "$REPORT_FILE" || true
  else
    echo "Gradle not installed; skipping deep checks." >> "$REPORT_FILE"
  fi
else
  echo "## No Maven/Gradle build detected — skipping build checks" >> "$REPORT_FILE"
fi

echo "" >> "$REPORT_FILE"

# 3) Simple Java patterns: large files
echo "## Simple heuristics" >> "$REPORT_FILE"
find . -name '*.java' -type f -exec wc -l {} + | sort -n -r | head -n 10 >> "$REPORT_FILE" || true

echo "" >> "$REPORT_FILE"

# 4) If FOUND, create a local issue file and optionally call GitHub API (if GITHUB_TOKEN present)
if [ $FOUND -ne 0 ]; then
  ISSUE_TITLE="Automated review: potential improvements ($TIMESTAMP)"
  echo "---" > "$REPORT_FILE.tmp"
  echo "title: $ISSUE_TITLE" >> "$REPORT_FILE.tmp"
  echo "---" >> "$REPORT_FILE.tmp"
  cat "$REPORT_FILE" >> "$REPORT_FILE.tmp"
  mv "$REPORT_FILE.tmp" "$REPORT_FILE"
  echo "[AUTOMATED] Review produced findings. Report saved to $REPORT_FILE"
  # If GITHUB_TOKEN is present and remote is GitHub, try to open an issue
  if [ -n "${GITHUB_TOKEN-}" ] && echo "$REMOTE_URL" | grep -q "github.com"; then
    OWNER_REPO=$(echo "$REMOTE_URL" | sed -e 's#.*github.com[:/]##' -e 's/\.git$//')
    echo "Creating GitHub issue on $OWNER_REPO"
    API_URL="https://api.github.com/repos/$OWNER_REPO/issues"
    BODY=$(jq -Rs --arg f "$ISSUE_TITLE" '{title:$f, body: input}' < "$REPORT_FILE")
    curl -s -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/json" -d "$BODY" "$API_URL" > /tmp/gh_issue.out || true
    if grep -q '"html_url"' /tmp/gh_issue.out 2>/dev/null; then
      echo "GitHub issue created: " $(jq -r '.html_url' /tmp/gh_issue.out) >> "$REPORT_FILE"
    else
      echo "Failed to create GitHub issue. Response:" >> "$REPORT_FILE"
      sed -n '1,200p' /tmp/gh_issue.out >> "$REPORT_FILE"
    fi
  else
    echo "No GITHUB_TOKEN or remote not GitHub — saved local report only." >> "$REPORT_FILE"
  fi
else
  rm -f "$REPORT_FILE"
  echo "No issues detected by automated checks."
fi

exit 0
