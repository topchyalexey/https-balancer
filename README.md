# https-balancer

# Генерация keystore для балансировщика
keytool -genkeypair -alias balance -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650

# Генерация truststore (если используете самоподписанные сертификаты на backend)
keytool -import -alias backend1 -file backend1.crt -keystore truststore.p12 -storetype PKCS12

# сгенерировать тестовый keystore командой:
keytool -genkeypair -alias balance -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 3650

# отправка POST
curl -X POST https://localhost:8443/balance/api/data \
-H "Content-Type: application/json" \
-H "Authorization: Bearer token" \
-d '{"key":"value"}'