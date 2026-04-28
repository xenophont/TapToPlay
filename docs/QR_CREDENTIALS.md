# Perfiles de Credenciales QR

TapToPlay puede importar perfiles de pago de Adyen escaneando un QR que contiene un payload JSON. Los perfiles se guardan en almacenamiento local cifrado y se seleccionan explicitamente en la app.

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

## Reglas de Validacion

- `schema` debe ser `taptoplay.adyen.profile.v1`.
- `environment` debe ser `test` o `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, los campos de terminal key, `currency` y `countryCode` son obligatorios.
- `storeId` es opcional. Cuando esta presente, TapToPlay solicita boarding enrutado por store.
- `currency` debe ser un codigo ISO 4217 en mayusculas, como `EUR`.
- `countryCode` debe ser un codigo ISO 3166-1 alpha-2 en mayusculas, como `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion` y `terminalPassphrase` deben coincidir con la shared key configurada en Adyen Customer Area para el cifrado de Terminal API.

## Crear un QR

Usa cualquier generador QR de confianza que acepte texto en crudo, pega el payload JSON y genera el codigo QR. Trata la imagen QR como un secreto porque contiene API keys y material de cifrado de terminal.

Para demos parecidas a produccion, genera codigos QR separados para perfiles test y live para que el cambio siga siendo explicito en la app.

## Codigos QR de SaleToAcquirerData

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
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION",
  "store": "YOUR_STORE_REFERENCE",
  "tenderOption": "ReceiptHandler,AskGratuity"
}
```

Reglas:

- El objeto raiz es el propio objeto SaleToAcquirerData.
- TapToPlay codifica este objeto exactamente como se ha escaneado en Base64 y lo envia como `PaymentRequest.SaleData.SaleToAcquirerData`.
- Los QR antiguos con `schema`, `displayName` y `saleToAcquirerData` o `properties` siguen siendo aceptados, pero los QR nuevos deberian usar el objeto plano.
- Los valores pueden ser strings, numeros, booleanos, arrays u objetos anidados.
