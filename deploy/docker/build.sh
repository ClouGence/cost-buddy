#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

IMAGE_REPOSITORY="${IMAGE_REPOSITORY:-cloudcanal-registry.cn-shanghai.cr.aliyuncs.com/clougence/cost-buddy}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
PLATFORM="${PLATFORM:-linux/amd64}"
TAG_ARCH_SUFFIX="${TAG_ARCH_SUFFIX:-true}"
OUTPUT_DIR="${OUTPUT_DIR:-${PROJECT_ROOT}/deploy/dist}"

GRADLE_CMD="${GRADLE_CMD:-}"
RUNTIME_IMAGE="${RUNTIME_IMAGE:-registry.cn-hangzhou.aliyuncs.com/cloudcanal/baseimage:centos8_x86_v2}"
COST_BUDDY_UID="${COST_BUDDY_UID:-10001}"
COST_BUDDY_GID="${COST_BUDDY_GID:-10001}"

if [[ -z "${GRADLE_CMD}" ]]; then
  if [[ -x "${PROJECT_ROOT}/gradlew" ]]; then
    GRADLE_CMD="${PROJECT_ROOT}/gradlew"
  else
    GRADLE_CMD="gradle"
  fi
fi

arch_suffix() {
  case "${PLATFORM}" in
    linux/amd64) echo "amd64" ;;
    linux/arm64) echo "arm64" ;;
    *) echo "${PLATFORM##*/}" | tr '/' '-' ;;
  esac
}

checksum() {
  local file="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${file}"
  else
    shasum -a 256 "${file}"
  fi
}

if [[ "${TAG_ARCH_SUFFIX}" == "true" ]]; then
  FULL_TAG="${IMAGE_REPOSITORY}:${IMAGE_TAG}-$(arch_suffix)"
  IMAGE_FILE_TAG="${IMAGE_TAG}-$(arch_suffix)"
else
  FULL_TAG="${IMAGE_REPOSITORY}:${IMAGE_TAG}"
  IMAGE_FILE_TAG="${IMAGE_TAG}"
fi

IMAGE_TAR="${OUTPUT_DIR}/cost-buddy-${IMAGE_FILE_TAG}.image.tar"
BUILD_CONTEXT="$(mktemp -d)"
trap 'rm -rf "${BUILD_CONTEXT}"' EXIT

BUILD_ARGS=(
  --platform "${PLATFORM}"
  --file "${SCRIPT_DIR}/Dockerfile"
  --build-arg "RUNTIME_IMAGE=${RUNTIME_IMAGE}"
  --build-arg "COST_BUDDY_UID=${COST_BUDDY_UID}"
  --build-arg "COST_BUDDY_GID=${COST_BUDDY_GID}"
  --label "org.opencontainers.image.title=cost-buddy"
  --label "org.opencontainers.image.source=cost-buddy"
  --tag "${FULL_TAG}"
)

mkdir -p "${OUTPUT_DIR}"
cd "${PROJECT_ROOT}"

(
  cd "${PROJECT_ROOT}/frontend"
  npm ci
  npm run build
)

"${GRADLE_CMD}" clean build -x test --no-daemon

JAR_FILE="$(find "${PROJECT_ROOT}/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | sort | tail -n 1)"
if [[ -z "${JAR_FILE}" ]]; then
  echo "cost-buddy boot jar not found in ${PROJECT_ROOT}/build/libs" >&2
  exit 1
fi

cp "${JAR_FILE}" "${BUILD_CONTEXT}/cost-buddy.jar"
cp "${SCRIPT_DIR}/docker-entrypoint.sh" "${BUILD_CONTEXT}/docker-entrypoint.sh"

docker build "${BUILD_ARGS[@]}" "${BUILD_CONTEXT}"
docker save --output "${IMAGE_TAR}" "${FULL_TAG}"
checksum "${IMAGE_TAR}" > "${IMAGE_TAR}.sha256"
cp "${PROJECT_ROOT}/docker-compose.yml" "${OUTPUT_DIR}/docker-compose.yml"
cp "${PROJECT_ROOT}/.env.example" "${OUTPUT_DIR}/.env.example"
cp "${SCRIPT_DIR}/install.sh" "${OUTPUT_DIR}/install.sh"
chmod +x "${OUTPUT_DIR}/install.sh"

echo "Built image: ${FULL_TAG}"
echo "Saved image package: ${IMAGE_TAR}"
echo "Saved checksum: ${IMAGE_TAR}.sha256"
echo "Saved deploy files: ${OUTPUT_DIR}/docker-compose.yml ${OUTPUT_DIR}/.env.example ${OUTPUT_DIR}/install.sh"
