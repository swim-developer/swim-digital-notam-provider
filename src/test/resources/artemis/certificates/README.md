## Certificates
```shell
oc get secret mockserver-client-tls -n swim-sandbox -o jsonpath='{.data}' | jq -r '."tls.key"' | base64 -d > client.key
oc get secret mockserver-client-tls -n swim-sandbox -o jsonpath='{.data}' | jq -r '."tls.crt"' | base64 -d > client.crt
oc get secret mockserver-client-tls -n swim-sandbox -o jsonpath='{.data}' | jq -r '."ca.crt"' | base64 -d > ca.crt
```

## Client Keystore
```shell
openssl pkcs12 -export \
    -in client.crt \
    -inkey client.key \
    -name "mockserver-client" \
    -out client-keystore.p12 \
    -passout pass:password
```

## Client Truststore
```shell
keytool -import \
    -alias root-ca \
    -file ca.crt \
    -keystore client-truststore.jks \
    -storepass password \
    -noprompt
```