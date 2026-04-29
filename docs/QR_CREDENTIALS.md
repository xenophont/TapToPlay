# QR Credential Profiles

TapToPlay can import Adyen payment profiles by scanning a QR code that contains a JSON payload. Profiles are stored in encrypted local storage and selected explicitly in the app.

## JSON Format

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Demo Store TEST",
  "environment": "test",
  "merchantId": "YourMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "AQE...",
  "clientKey": "test_...",
  "terminalKeyIdentifier": "CryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

## Live Example

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Boutique Madrid LIVE",
  "environment": "live",
  "merchantId": "YourLiveMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "live_AQE...",
  "clientKey": "live_...",
  "terminalKeyIdentifier": "LiveCryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "live-shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

## Validation Rules

- `schema` must be `taptoplay.adyen.profile.v1`.
- `environment` must be `test` or `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, terminal key fields, `currency`, and `countryCode` are required.
- `storeId` is optional. When present, TapToPlay requests store-routed boarding.
- `currency` must be an uppercase ISO 4217 code such as `EUR`.
- `countryCode` must be an uppercase ISO 3166-1 alpha-2 code such as `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion`, and `terminalPassphrase` must match the shared key configured in the Adyen Customer Area for Terminal API encryption.

## Creating a QR Code

Use any trusted QR generator that accepts raw text, paste the JSON payload, and generate a QR code. Treat the QR image like a secret because it contains API keys and terminal encryption material.

For production-like demos, generate separate QR codes for test and live profiles so switching remains explicit in the app.

## SaleToAcquirerData QR Codes

Credential QR codes use `taptoplay.adyen.profile.v1`. SaleToAcquirerData QR codes should contain the plain Adyen SaleToAcquirerData JSON object so they can be scanned from checkout without changing the active payment profile.

```json
{
  "applicationInfo": {
    "externalPlatform": {
      "name": "COMPANY_NAME_OR_PLATFORM_NAME",
      "version": "1.3",
      "integrator": "COMPANY_THAT_BUILT_INTEGRATION_OR_POS_APP"
    },
    "merchantApplication": {
      "name": "NAME_OF_POS_APPLICATION",
      "version": "2.13.05"
    },
    "merchantDevice": {
      "os": "OS_OF_DEVICE_THAT_RUNS_POS_APPLICATION",
      "osVersion": "16.3"
    }
  },
  "metadata": {
    "someMetaDataKey1": "YOUR_VALUE",
    "someMetaDataKey2": "YOUR_VALUE"
  },
  "shopperEmail": "S.Hopper@example.com",
  "shopperReference": "YOUR_UNIQUE_SHOPPER_ID",
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION"
}
```

Rules:

- The root object is the SaleToAcquirerData object itself.
- TapToPlay Base64-encodes this object exactly as scanned and sends it as `PaymentRequest.SaleData.SaleToAcquirerData`.
- Legacy QR codes with `schema`, `displayName`, and `saleToAcquirerData` or `properties` are still accepted, but newly generated QRs should use the plain object shape.
- Values may be strings, numbers, booleans, arrays, or nested objects.

---

## Español

# Perfiles de Credenciales QR

TapToPlay puede importar perfiles de pago de Adyen escaneando un QR que contiene un payload JSON. Los perfiles se guardan en almacenamiento local cifrado y se seleccionan explícitamente en la app.

## Formato JSON

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Demo Store TEST",
  "environment": "test",
  "merchantId": "YourMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "AQE...",
  "clientKey": "test_...",
  "terminalKeyIdentifier": "CryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

## Ejemplo Live

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Boutique Madrid LIVE",
  "environment": "live",
  "merchantId": "YourLiveMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "live_AQE...",
  "clientKey": "live_...",
  "terminalKeyIdentifier": "LiveCryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "live-shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

## Reglas de Validación

- `schema` debe ser `taptoplay.adyen.profile.v1`.
- `environment` debe ser `test` o `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, los campos de terminal key, `currency` y `countryCode` son obligatorios.
- `storeId` es opcional. Cuando está presente, TapToPlay solicita boarding enrutado por store.
- `currency` debe ser un código ISO 4217 en mayúsculas, como `EUR`.
- `countryCode` debe ser un código ISO 3166-1 alpha-2 en mayúsculas, como `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion` y `terminalPassphrase` deben coincidir con la shared key configurada en Adyen Customer Area para el cifrado de Terminal API.

## Crear un QR

Usa cualquier generador QR de confianza que acepte texto en crudo, pega el payload JSON y genera el código QR. Trata la imagen QR como un secreto porque contiene API keys y material de cifrado de terminal.

Para demos parecidas a producción, genera códigos QR separados para perfiles test y live para que el cambio siga siendo explícito en la app.

## Códigos QR de SaleToAcquirerData

Los QR de credenciales usan `taptoplay.adyen.profile.v1`. Los QR de SaleToAcquirerData deben contener el objeto JSON plano de Adyen SaleToAcquirerData para poder escanearlos desde checkout sin cambiar el perfil de pago activo.

```json
{
  "applicationInfo": {
    "externalPlatform": {
      "name": "COMPANY_NAME_OR_PLATFORM_NAME",
      "version": "1.3",
      "integrator": "COMPANY_THAT_BUILT_INTEGRATION_OR_POS_APP"
    },
    "merchantApplication": {
      "name": "NAME_OF_POS_APPLICATION",
      "version": "2.13.05"
    },
    "merchantDevice": {
      "os": "OS_OF_DEVICE_THAT_RUNS_POS_APPLICATION",
      "osVersion": "16.3"
    }
  },
  "metadata": {
    "someMetaDataKey1": "YOUR_VALUE",
    "someMetaDataKey2": "YOUR_VALUE"
  },
  "shopperEmail": "S.Hopper@example.com",
  "shopperReference": "YOUR_UNIQUE_SHOPPER_ID",
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION"
}
```

Reglas:

- El objeto raíz es el propio objeto SaleToAcquirerData.
- TapToPlay codifica este objeto exactamente como se ha escaneado en Base64 y lo envía como `PaymentRequest.SaleData.SaleToAcquirerData`.
- Los QR antiguos con `schema`, `displayName` y `saleToAcquirerData` o `properties` siguen siendo aceptados, pero los QR nuevos deberían usar el objeto plano.
- Los valores pueden ser strings, números, booleanos, arrays u objetos anidados.
