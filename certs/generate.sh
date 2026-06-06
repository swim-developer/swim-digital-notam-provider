#!/bin/bash
# Generates local development certificates for swim-digital-notam-provider.
#
# This project is self-contained: no dependency on swim-developer-tools.
#
# Output (all files in certs/):
#   ca.crt                  CA certificate (mkcert root CA, PEM)
#   tls.crt                 Provider HTTPS server certificate (PEM)
#   tls.key                 Provider HTTPS server private key  (PEM)
#   client.crt              Provider AMQP client certificate  (PEM) — mTLS to Artemis
#   client.key              Provider AMQP client private key  (PEM)
#   broker.p12              Artemis broker keystore (PKCS12)
#   ca-truststore.p12       Artemis CA truststore   (PKCS12)  — verifies consumer client certs
#   validator-keystore.p12  Validator AMQP client keystore (PKCS12) — mTLS to Artemis
#   validator-truststore.p12 Validator CA truststore (PKCS12) — trusts broker + provider HTTPS
#
# Prerequisites:
#   mkcert   https://github.com/FiloSottile/mkcert  (brew install mkcert)
#   keytool  Bundled with JDK 21
#   openssl  Pre-installed on macOS / most Linux distros
#
# Usage (run from the project root):
#   ./certs/generate.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERTS_DIR="${SCRIPT_DIR}"
TMP_DIR="${CERTS_DIR}/.tmp"
PASSWORD="changeit"

# SANs for the Artemis broker server certificate.
# nip.io resolves embedded IPs automatically:
#   artemis.127.0.0.1.nip.io -> 127.0.0.1
# This avoids /etc/hosts entries for local development.
BROKER_SANS=(
    "localhost"
    "127.0.0.1"
    "::1"
    "dnotam-provider-artemis"
    "provider-artemis"
    "artemis.127.0.0.1.nip.io"
    "dnotam-provider-artemis.127.0.0.1.nip.io"
    "dnotam-provider-artemis.swim.lab"
    "provider-artemis.swim.lab"
)

# SANs for the Keycloak HTTPS server certificate.
KEYCLOAK_SANS=(
    "keycloak.swim.lab"
    "keycloak"
    "localhost"
    "127.0.0.1"
    "::1"
    "keycloak.127.0.0.1.nip.io"
)

# SANs for the provider HTTPS server certificate.
# Used for the Subscription Manager REST API and WFS endpoint.
PROVIDER_SANS=(
    "localhost"
    "127.0.0.1"
    "::1"
    "dnotam-provider"
    "provider.127.0.0.1.nip.io"
    "dnotam-provider.127.0.0.1.nip.io"
    "dnotam-provider.swim.lab"
)

# SANs for the provider client certificate.
# Used for mTLS identification when the provider app connects to its own Artemis.
CLIENT_SANS=(
    "dnotam-provider"
    "localhost"
    "127.0.0.1"
    "dnotam-provider.127.0.0.1.nip.io"
    "dnotam-provider.swim.lab"
)

# SANs for the validator client certificate.
# Used for mTLS identification when the provider-validator connects to the provider Artemis.
VALIDATOR_SANS=(
    "dnotam-provider-validator"
    "localhost"
    "127.0.0.1"
    "dnotam-provider-validator.127.0.0.1.nip.io"
    "dnotam-provider-validator.swim.lab"
)

# --- Cleanup ------------------------------------------------------------------

for f in ca.crt tls.crt tls.key client.crt client.key broker.p12 ca-truststore.p12 keycloak-keystore.p12 validator-keystore.p12 validator-truststore.p12; do
    rm -f "${CERTS_DIR}/${f}"
done

# --- Prerequisites ------------------------------------------------------------

check_command() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "ERROR: '$1' not found."
        echo "       $2"
        exit 1
    }
}

check_command mkcert  "Install: brew install mkcert  |  https://github.com/FiloSottile/mkcert"
check_command keytool "Install JDK 21: https://adoptium.net"
check_command openssl "Install: brew install openssl"

# --- Setup --------------------------------------------------------------------

echo ""
echo "=== swim-digital-notam-provider local PKI ==="
echo ""

mkcert -install

mkdir -p "${TMP_DIR}"
trap 'rm -rf "${TMP_DIR}"' EXIT

# CA certificate (mkcert root CA, shared across all local projects)
CA_ROOT="$(mkcert -CAROOT)"
cp "${CA_ROOT}/rootCA.pem" "${CERTS_DIR}/ca.crt"
echo "[CA] ca.crt  <-  $(mkcert -CAROOT)/rootCA.pem"

# --- Artemis broker server certificate ----------------------------------------

echo ""
echo "[broker] Generating Artemis server certificate..."
echo "         SANs: ${BROKER_SANS[*]}"

mkcert \
    -cert-file "${TMP_DIR}/broker.crt" \
    -key-file  "${TMP_DIR}/broker.key" \
    "${BROKER_SANS[@]}"

openssl pkcs12 -export \
    -in       "${TMP_DIR}/broker.crt" \
    -inkey    "${TMP_DIR}/broker.key" \
    -certfile "${CERTS_DIR}/ca.crt" \
    -out      "${CERTS_DIR}/broker.p12" \
    -name     broker \
    -password "pass:${PASSWORD}"

chmod 644 "${CERTS_DIR}/broker.p12"
echo "[broker] broker.p12  (keystore, password: ${PASSWORD})"

# --- Artemis CA truststore (verifies incoming client certs from consumers) ----

echo ""
echo "[artemis-truststore] Creating CA truststore for Artemis..."

keytool -importcert -noprompt \
    -alias     swim-ca \
    -file      "${CERTS_DIR}/ca.crt" \
    -keystore  "${CERTS_DIR}/ca-truststore.p12" \
    -storetype PKCS12 \
    -storepass "${PASSWORD}"

chmod 644 "${CERTS_DIR}/ca-truststore.p12"
echo "[artemis-truststore] ca-truststore.p12  (password: ${PASSWORD})"

# --- Provider HTTPS server certificate ----------------------------------------

echo ""
echo "[provider] Generating provider HTTPS server certificate..."
echo "           SANs: ${PROVIDER_SANS[*]}"

mkcert \
    -cert-file "${CERTS_DIR}/tls.crt" \
    -key-file  "${CERTS_DIR}/tls.key" \
    "${PROVIDER_SANS[@]}"

chmod 644 "${CERTS_DIR}/tls.crt" "${CERTS_DIR}/tls.key"
echo "[provider] tls.crt, tls.key  (PEM)"

# --- Keycloak HTTPS server certificate and keystore ---------------------------

echo ""
echo "[keycloak] Generating Keycloak HTTPS server certificate..."
echo "           SANs: ${KEYCLOAK_SANS[*]}"

mkcert \
    -cert-file "${TMP_DIR}/keycloak.crt" \
    -key-file  "${TMP_DIR}/keycloak.key" \
    "${KEYCLOAK_SANS[@]}"

openssl pkcs12 -export \
    -in       "${TMP_DIR}/keycloak.crt" \
    -inkey    "${TMP_DIR}/keycloak.key" \
    -certfile "${CERTS_DIR}/ca.crt" \
    -out      "${CERTS_DIR}/keycloak-keystore.p12" \
    -name     keycloak \
    -password "pass:${PASSWORD}"

chmod 644 "${CERTS_DIR}/keycloak-keystore.p12"
echo "[keycloak] keycloak-keystore.p12  (PKCS12, password: ${PASSWORD})"

# --- Provider AMQP client certificate -----------------------------------------

echo ""
echo "[client] Generating provider client certificate for AMQP..."
echo "         SANs: ${CLIENT_SANS[*]}"

mkcert \
    -client \
    -cert-file "${CERTS_DIR}/client.crt" \
    -key-file  "${CERTS_DIR}/client.key" \
    "${CLIENT_SANS[@]}"

chmod 644 "${CERTS_DIR}/client.crt" "${CERTS_DIR}/client.key"
echo "[client] client.crt, client.key  (PEM)"

# --- Validator AMQP client certificate and keystores --------------------------

echo ""
echo "[validator] Generating validator client certificate for AMQP..."
echo "            SANs: ${VALIDATOR_SANS[*]}"

mkcert \
    -client \
    -cert-file "${TMP_DIR}/validator-client.crt" \
    -key-file  "${TMP_DIR}/validator-client.key" \
    "${VALIDATOR_SANS[@]}"

openssl pkcs12 -export \
    -in       "${TMP_DIR}/validator-client.crt" \
    -inkey    "${TMP_DIR}/validator-client.key" \
    -certfile "${CERTS_DIR}/ca.crt" \
    -out      "${CERTS_DIR}/validator-keystore.p12" \
    -name     validator \
    -password "pass:${PASSWORD}"

chmod 644 "${CERTS_DIR}/validator-keystore.p12"
echo "[validator] validator-keystore.p12  (PKCS12, password: ${PASSWORD})"

keytool -importcert -noprompt \
    -alias     swim-ca \
    -file      "${CERTS_DIR}/ca.crt" \
    -keystore  "${CERTS_DIR}/validator-truststore.p12" \
    -storetype PKCS12 \
    -storepass "${PASSWORD}"

chmod 644 "${CERTS_DIR}/validator-truststore.p12"
echo "[validator] validator-truststore.p12  (PKCS12, password: ${PASSWORD})"

# --- Summary ------------------------------------------------------------------

cat <<SUMMARY

=== Done ===

  certs/
  ├── ca.crt                    CA certificate (mkcert root CA)
  ├── tls.crt                   Provider HTTPS server cert      (PEM)
  ├── tls.key                   Provider HTTPS server key       (PEM)
  ├── client.crt                Provider AMQP client cert       (PEM)
  ├── client.key                Provider AMQP client key        (PEM)
  ├── broker.p12                Artemis broker keystore         (PKCS12, password: ${PASSWORD})
  ├── ca-truststore.p12         Artemis CA truststore           (PKCS12, password: ${PASSWORD})
  ├── keycloak-keystore.p12     Keycloak HTTPS keystore         (PKCS12, password: ${PASSWORD})
  ├── validator-keystore.p12    Validator AMQP client keystore  (PKCS12, password: ${PASSWORD})
  └── validator-truststore.p12  Validator CA truststore         (PKCS12, password: ${PASSWORD})

  Broker SANs    : ${BROKER_SANS[*]}
  Keycloak SANs  : ${KEYCLOAK_SANS[*]}  (keycloak.127.0.0.1.nip.io resolves without /etc/hosts — but /etc/hosts is still needed for container-internal JWT validation)
  Provider SANs  : ${PROVIDER_SANS[*]}
  Client SANs    : ${CLIENT_SANS[*]}
  Validator SANs : ${VALIDATOR_SANS[*]}

  All keystore / truststore password: ${PASSWORD}

  Optional — add to /etc/hosts for swim.lab resolution:
    127.0.0.1  keycloak.swim.lab
    127.0.0.1  provider-artemis.swim.lab
    127.0.0.1  dnotam-provider-artemis.swim.lab
    127.0.0.1  dnotam-provider.swim.lab
    127.0.0.1  dnotam-provider-validator.swim.lab

SUMMARY
