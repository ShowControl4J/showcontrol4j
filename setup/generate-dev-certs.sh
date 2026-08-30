#!/usr/bin/env bash
set -euo pipefail

# Generates a self-signed CA and server certificate for the LOCAL DEVELOPMENT
# Mosquitto broker only. Never reuse these certificates outside a trusted
# local/home network, and never commit the generated .certs/ directory - it
# contains private keys. See ROADMAP.md, Phase 1, "Mosquitto secured with
# auth + TLS from day one" for why, and what production TLS needs instead
# (a certificate from a real, publicly trusted CA).
#
# Usage: ./generate-dev-certs.sh [hostname]
#   hostname defaults to "localhost" - pass the actual hostname/IP show
#   elements and triggers will use to reach the broker if it's not localhost.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="${SCRIPT_DIR}/.certs"
DAYS_VALID=825
BROKER_HOST="${1:-localhost}"

mkdir -p "$CERT_DIR"
cd "$CERT_DIR"

echo "Generating a dev-only CA + server certificate for host '${BROKER_HOST}' in ${CERT_DIR}"

openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days "$DAYS_VALID" \
    -subj "/CN=ShowControl4J Dev CA" -out ca.crt

openssl genrsa -out server.key 4096
openssl req -new -key server.key -subj "/CN=${BROKER_HOST}" -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
    -out server.crt -days "$DAYS_VALID" -sha256 \
    -extfile <(printf "subjectAltName=DNS:%s" "$BROKER_HOST")

rm -f server.csr ca.srl
chmod 600 ca.key server.key

echo "Done."
echo "  - ca.crt, server.crt, server.key -> used by Mosquitto (mounted via docker-compose.yml)"
echo "  - ca.crt is also what client code should trust: BrokerConnectionFactory.Builder.tls(new File(...))"
