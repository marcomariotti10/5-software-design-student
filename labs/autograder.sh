#!/usr/bin/env bash
set -euo pipefail

# Determine whether we're in teacher repo (root/solutions) or student repo (root/labs)
# POSIX bash (macOS/Linux) only
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR_NAME="$(basename "$SCRIPT_DIR")"

if [[ "$BASE_DIR_NAME" == "solutions" ]]; then
  SOL_DIR="$SCRIPT_DIR"
  ROOT_DIR="$( dirname "$SOL_DIR" )"
  # Prefer the repo root aggregator if present (this brings in shared modules like tools/annotations)
  if [[ -f "$ROOT_DIR/pom.xml" ]]; then
    AGG_POM="$ROOT_DIR/pom.xml"
  else
    AGG_POM="$SOL_DIR/pom.xml"
  fi
  # Module selector relative to the AGG_POM location
  if [[ "$AGG_POM" == "$ROOT_DIR/pom.xml" ]]; then
    MODULE_SELECTOR="$(basename "$SOL_DIR")/grader"
  else
    MODULE_SELECTOR="grader"
  fi
  MODULE_DIR="$SOL_DIR/grader"
  MAIN="be.uantwerpen.sd.solutions.grader.GraderMain"
elif [[ "$BASE_DIR_NAME" == "labs" ]]; then
  SOL_DIR="$SCRIPT_DIR"
  ROOT_DIR="$( dirname "$SOL_DIR" )"
  # Prefer the repo root aggregator if present (this brings in shared modules like tools/annotations)
  if [[ -f "$ROOT_DIR/pom.xml" ]]; then
    AGG_POM="$ROOT_DIR/pom.xml"
  else
    AGG_POM="$SOL_DIR/pom.xml"
  fi
  # Module selector relative to the AGG_POM location
  if [[ "$AGG_POM" == "$ROOT_DIR/pom.xml" ]]; then
    MODULE_SELECTOR="$(basename "$SOL_DIR")/grader"
  else
    MODULE_SELECTOR="grader"
  fi
  MODULE_DIR="$SOL_DIR/grader"
  MAIN="be.uantwerpen.sd.labs.grader.GraderMain"
else
  echo "[autograder] ERROR: this script must live in either root/solutions or root/labs."
  echo "[autograder] Found at: $SCRIPT_DIR (basename: $BASE_DIR_NAME)"
  exit 1
fi

MODULE_POM="$MODULE_DIR/pom.xml"

 # Prefer wrapper at repo root
MVN="$ROOT_DIR/mvnw"; [[ -x "$MVN" ]] || MVN="mvn"

# Fresh, isolated Maven local repo each run (no cross-run contamination)
REPO_DIR="$ROOT_DIR/.m2-autograder"
rm -rf "$REPO_DIR"
mkdir -p "$REPO_DIR"
MVN_COMMON_FLAGS=(-Dmaven.repo.local="$REPO_DIR" -Dmaven.test.skip=true -U)

echo "[autograder] LOCAL_M2=$REPO_DIR"

echo "[autograder] ROOT_DIR=$ROOT_DIR"
echo "[autograder] AGG_POM=$AGG_POM"
echo "[autograder] MODULE_DIR=$MODULE_DIR"
echo "[autograder] MAIN=$MAIN"
echo "[autograder] MODULE_SELECTOR=$MODULE_SELECTOR"

echo "[autograder] mvn + java versions:"
"$MVN" -v || true
java -version || true

echo "[autograder] building reactor (grader + labs)…"
# (drop -q so we can see errors)
"$MVN" -q -f "$AGG_POM" -pl "$MODULE_SELECTOR" -am "${MVN_COMMON_FLAGS[@]}" install

echo "[autograder] running grader…"
# Normalize "lab" shorthand into GraderMain's --lab/--lab= forms (supports "lab=labX" and "lab labX")
declare -a FORWARDED=()
expect_lab_value=0
for arg in "$@"; do
  if [[ $expect_lab_value -eq 1 ]]; then
    FORWARDED+=("--lab" "$arg")
    expect_lab_value=0
    continue
  fi

  if [[ "$arg" =~ ^lab=(.+)$ ]]; then
    FORWARDED+=("--lab=${BASH_REMATCH[1]}")
  elif [[ "$arg" == "lab" ]]; then
    expect_lab_value=1
  else
    FORWARDED+=("$arg")
  fi
done
if [[ $expect_lab_value -eq 1 ]]; then
  echo "[autograder] ERROR: 'lab' provided without a value. Usage: lab=lab1 or lab lab1"
  exit 2
fi

# Properly pass program args to exec:java (handle empty array under set -u)
if ((${#FORWARDED[@]})); then
  ARGS_JOINED=$(printf ' %q' "${FORWARDED[@]}")
  ARGS_JOINED=${ARGS_JOINED:1}
else
  ARGS_JOINED=""
fi
echo "[autograder] exec args: $ARGS_JOINED"

# Run exec:java from the grader module (dependencies were installed to ~/.m2 above)
cd "$MODULE_DIR"
"$MVN" -q -f "$MODULE_POM" -e "${MVN_COMMON_FLAGS[@]}" exec:java \
  -Dexec.classpathScope=runtime \
  -Dexec.mainClass="$MAIN" \
  -Dexec.workingdir="$MODULE_DIR" \
  -Dexec.args="$ARGS_JOINED"