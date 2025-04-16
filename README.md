# https-balancer

# Генерация keystore для балансировщика
keytool -genkeypair -alias balance -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650

# Генерация truststore (если используете самоподписанные сертификаты на backend)
keytool -import -alias backend1 -file backend1.crt -keystore truststore.p12 -storetype PKCS12