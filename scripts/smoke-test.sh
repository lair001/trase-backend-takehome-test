#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

check_public_endpoint() {
  local path="$1"
  local allow_redirect="${2:-false}"
  local response status body
  response=$(curl -sS -w "\n%{http_code}" "$BASE_URL$path" 2>/dev/null || true)
  body="${response%$'\n'*}"
  status="${response##*$'\n'}"
  if [[ -z "$status" || "$status" == "000" ]]; then
    echo "Public endpoint check failed ($path): no response" >&2
    exit 1
  fi
  if [[ "$allow_redirect" == "true" ]]; then
    if [[ ! "$status" =~ ^2|3 ]]; then
      echo "Public endpoint check failed ($path) status=$status body=$body" >&2
      exit 1
    fi
  else
    if [[ "$status" != "200" ]]; then
      echo "Public endpoint check failed ($path) status=$status body=$body" >&2
      exit 1
    fi
  fi
  if [[ "$path" == "/v3/api-docs" && "$body" != *"\"openapi\""* ]]; then
    echo "Public endpoint check failed ($path): missing openapi in response" >&2
    exit 1
  fi
}

login_response=""
login_status=""
login_body=""
max_attempts=30
attempt=1

while [[ $attempt -le $max_attempts ]]; do
  login_response=$(curl -sS -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123!"}' 2>/dev/null || true)
  if [[ -n "$login_response" ]]; then
    login_body="${login_response%$'\n'*}"
    login_status="${login_response##*$'\n'}"
    if [[ "$login_status" == "200" ]]; then
      break
    fi
  fi
  printf "Waiting for API... (%d/%d)\\n" "$attempt" "$max_attempts"
  sleep 2
  attempt=$((attempt + 1))
done

if [[ "$login_status" != "200" ]]; then
  echo "Login failed (${login_status:-no status}): ${login_body:-no response}" >&2
  exit 1
fi

token=$(python - "$login_body" <<'PY'
import json,sys
print(json.loads(sys.argv[1])["accessToken"])
PY
)

if [[ -z "$token" ]]; then
  echo "Login response missing accessToken" >&2
  exit 1
fi

tmp_request() {
  local method="$1"
  local path="$2"
  local data="${3-}"
  local response
  if [[ -n "$data" ]]; then
    response=$(curl -sS -w "\n%{http_code}" -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: application/json" \
      -d "$data")
  else
    response=$(curl -sS -w "\n%{http_code}" -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $token")
  fi
  HTTP_BODY="${response%$'\n'*}"
  HTTP_STATUS="${response##*$'\n'}"
}

printf "Running smoke test against %s\n" "$BASE_URL"

# Public docs should be accessible without auth
check_public_endpoint "/swagger-ui.html" "true"
check_public_endpoint "/v3/api-docs"

# Create agent
request_body='{"name":"Smoke Agent","description":"Smoke test"}'
tmp_request POST "/agents" "$request_body"
if [[ "$HTTP_STATUS" != "201" ]]; then
  echo "Create agent failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

agent_id=$(python - "$HTTP_BODY" <<'PY'
import json,sys
print(json.loads(sys.argv[1])["id"])
PY
)

# Create task
request_body=$(cat <<JSON
{"title":"Smoke Task","description":"Smoke test","supportedAgentIds":[${agent_id}]}
JSON
)

tmp_request POST "/tasks" "$request_body"
if [[ "$HTTP_STATUS" != "201" ]]; then
  echo "Create task failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

task_id=$(python - "$HTTP_BODY" <<'PY'
import json,sys
payload=json.loads(sys.argv[1])
print(payload["id"])
if payload.get("supportedAgentId") != payload.get("supportedAgentIds")[0]:
    raise SystemExit("supportedAgentId mismatch")
PY
)

# Fetch via /task alias
_tmp_path="/task/$task_id"
tmp_request GET "$_tmp_path"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Fetch task via alias failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

# List via /task alias
_tmp_path="/task"
tmp_request GET "$_tmp_path"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "List tasks via alias failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

# Delete via /task alias
_tmp_path="/task/$task_id"
tmp_request DELETE "$_tmp_path"
if [[ "$HTTP_STATUS" != "204" ]]; then
  echo "Delete task via alias failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

# Delete agent
_tmp_path="/agents/$agent_id"
tmp_request DELETE "$_tmp_path"
if [[ "$HTTP_STATUS" != "204" ]]; then
  echo "Delete agent failed ($HTTP_STATUS): $HTTP_BODY" >&2
  exit 1
fi

printf "Smoke test passed.\n"
