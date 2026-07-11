#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env"
ENV_EXAMPLE="${ROOT_DIR}/.env.example"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"
IMAGE_PACKAGE="${IMAGE_PACKAGE:-}"

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    echo "docker-compose"
    return
  fi
  echo "docker compose or docker-compose is required" >&2
  exit 1
}

find_image_package() {
  if [[ -n "${IMAGE_PACKAGE}" ]]; then
    echo "${IMAGE_PACKAGE}"
    return
  fi
  local found
  found="$(find "${ROOT_DIR}" -maxdepth 1 -type f -name 'cost-buddy-*.image.tar' | sort | tail -n 1)"
  if [[ -z "${found}" ]]; then
    echo "cost-buddy image package not found in ${ROOT_DIR}" >&2
    echo "Set IMAGE_PACKAGE=/path/to/cost-buddy-xxx.image.tar to install explicitly." >&2
    exit 1
  fi
  echo "${found}"
}

ensure_env_file() {
  if [[ -f "${ENV_FILE}" ]]; then
    return
  fi
  if [[ -f "${ENV_EXAMPLE}" ]]; then
    cp "${ENV_EXAMPLE}" "${ENV_FILE}"
    return
  fi
  echo ".env not found and .env.example is unavailable" >&2
  exit 1
}

upsert_env() {
  local key="$1"
  local value="$2"
  local tmp
  tmp="$(mktemp)"
  if grep -q "^${key}=" "${ENV_FILE}"; then
    awk -v key="${key}" -v value="${value}" 'BEGIN { prefix = key "=" } index($0, prefix) == 1 { print key "=" value; next } { print }' "${ENV_FILE}" > "${tmp}"
  else
    cp "${ENV_FILE}" "${tmp}"
    printf '%s=%s\n' "${key}" "${value}" >> "${tmp}"
  fi
  mv "${tmp}" "${ENV_FILE}"
}

env_value() {
  local key="$1"
  awk -F '=' -v key="${key}" 'index($0, key "=") == 1 { sub(/^[^=]*=/, ""); print; exit }' "${ENV_FILE}"
}

prepare_dir_if_configured() {
  local value="$1"
  local uid="$2"
  local gid="$3"
  if [[ -z "${value}" ]]; then
    return
  fi
  mkdir -p "${value}"
  if [[ -n "${uid}" && -n "${gid}" ]]; then
    if ! chown -R "${uid}:${gid}" "${value}" 2>/dev/null; then
      echo "warn: failed to chown ${value} to ${uid}:${gid}; container entrypoint will try to fix it on startup" >&2
    fi
  fi
}

prepare_log_file() {
  local log_dir="$1"
  local uid="$2"
  local gid="$3"
  if [[ -z "${log_dir}" ]]; then
    return
  fi
  local log_file="${log_dir}/cost-buddy.log"
  if ! touch "${log_file}" 2>/dev/null; then
    echo "warn: failed to touch ${log_file}; container entrypoint will try to create it on startup" >&2
    return
  fi
  if [[ -n "${uid}" && -n "${gid}" ]]; then
    if ! chown "${uid}:${gid}" "${log_file}" 2>/dev/null; then
      echo "warn: failed to chown ${log_file} to ${uid}:${gid}; container entrypoint will try to fix it on startup" >&2
    fi
  fi
  chmod u+rw "${log_file}"
}

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "docker-compose.yml not found: ${COMPOSE_FILE}" >&2
  exit 1
fi

PACKAGE_FILE="$(find_image_package)"
ensure_env_file

LOAD_OUTPUT="$(docker load --input "${PACKAGE_FILE}")"
echo "${LOAD_OUTPUT}"
LOADED_IMAGE="$(printf '%s\n' "${LOAD_OUTPUT}" | awk -F ': ' '/Loaded image:/ { print $2; exit }')"

if [[ -n "${LOADED_IMAGE}" ]]; then
  upsert_env "COST_BUDDY_IMAGE" "${LOADED_IMAGE}"
fi

COST_BUDDY_UID_VALUE="$(env_value COST_BUDDY_UID)"
COST_BUDDY_GID_VALUE="$(env_value COST_BUDDY_GID)"
COST_BUDDY_UID_VALUE="${COST_BUDDY_UID_VALUE:-10001}"
COST_BUDDY_GID_VALUE="${COST_BUDDY_GID_VALUE:-10001}"
COST_BUDDY_LOG_DIR_VALUE="$(env_value COST_BUDDY_LOG_DIR)"
prepare_dir_if_configured "${COST_BUDDY_LOG_DIR_VALUE}" "${COST_BUDDY_UID_VALUE}" "${COST_BUDDY_GID_VALUE}"
prepare_log_file "${COST_BUDDY_LOG_DIR_VALUE}" "${COST_BUDDY_UID_VALUE}" "${COST_BUDDY_GID_VALUE}"

COMPOSE="$(compose_cmd)"
${COMPOSE} --env-file "${ENV_FILE}" -f "${COMPOSE_FILE}" up -d --force-recreate

echo "Cost Buddy deploy finished."
