# QR Credential Profiles

TapToPlay imports Adyen payment profiles by scanning a QR code that contains raw JSON. Profiles are stored in encrypted local storage and must be selected deliberately in the `Payments App` tab.

Treat every credential QR as a secret. It contains API credentials and Terminal API encryption material.

## Credential JSON Format

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

## Live Example Shape

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

Use placeholder values in documentation and examples. Never commit a real live payload.

## Validation Rules

- `schema` must be `taptoplay.adyen.profile.v1`.
- Payloads larger than 8192 characters are rejected.
- `environment` must be `test` or `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, terminal key fields, `currency`, and `countryCode` are required.
- `displayName` must be 80 characters or less.
- `merchantId`, `storeId`, and `terminalKeyIdentifier` must be 128 characters or less.
- `apiKey`, `clientKey`, and `terminalPassphrase` must be 512 characters or less.
- `storeId` is optional. When present, TapToPlay requests store-scoped boarding and Payments App instance listing.
- `currency` must be an uppercase ISO 4217 code such as `EUR`.
- `countryCode` must be an uppercase ISO 3166-1 alpha-2 code such as `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion`, and `terminalPassphrase` must match the shared key configured in the Adyen Customer Area for Terminal API encryption.

Unknown fields are rejected for credential QR payloads. This keeps the credential schema tight and prevents accidental extra data from becoming part of a stored profile.

## Creating and Using a Credential QR

1. Fill the JSON with your Adyen test or live values.
2. Generate a QR code from the raw JSON text using a trusted offline QR tool, or an internal tool you control.
3. Do not upload real credential payloads to public QR services.
4. Open `Payments App` in TapToPlay.
5. Tap `Scan QR`.
6. Select the scanned profile deliberately.
7. Run `Check` and `Board` before charging.

For production-like demos, generate separate QR codes for test and live profiles so switching remains explicit.

## Credential Lifecycle

TapToPlay stores scanned profiles in encrypted preferences and masks secrets in the UI. The `Payments App` tab includes:

- `Remove`: deletes the local profile and clears saved local boarding state for that profile.
- `Refresh`: lists Adyen Payments App instances using the selected profile API key.
- `Revoke instance`: revokes a selected Adyen Payments App installation after confirmation.

Removing a profile does not revoke an Adyen Payments App installation. Revoke the instance separately when you need to invalidate the boarded app/device.

## SaleToAcquirerData QR Codes

Credential QR codes use `taptoplay.adyen.profile.v1`. SaleToAcquirerData QR codes are separate and should contain the plain Adyen SaleToAcquirerData JSON object so they can be scanned from `Checkout` without changing the active payment profile.

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
- Payloads larger than 12288 characters are rejected.
- `displayName`, when supplied by a legacy wrapper, must be 80 characters or less.
- The parsed object can contain at most 80 leaf fields.
- Legacy QR codes with `schema`, `displayName`, and `saleToAcquirerData` or `properties` are still accepted, but newly generated QRs should use the plain object shape.
- Values may be strings, numbers, booleans, arrays, or nested objects.

## Storage and Backup Notes

The app disables Android backup and explicitly excludes encrypted preference files used for profiles, boarding state, transaction history, and SaleToAcquirerData favorites. This reduces accidental credential movement between devices, but it does not turn an in-app credential model into a production architecture.

---

## Español

TapToPlay importa perfiles de pago de Adyen escaneando un QR con JSON en crudo. Cada QR de credenciales es secreto: contiene API keys y material de cifrado de Terminal API.

Reglas principales:

- `schema` debe ser `taptoplay.adyen.profile.v1`.
- `environment` debe ser `test` o `live`.
- El payload de credenciales no puede superar 8192 caracteres.
- Los secretos se guardan cifrados y se muestran enmascarados.
- `Remove` borra el perfil local, pero no revoca la instalación en Adyen.
- `Revoke instance` invalida una instalación de Payments App en Adyen y requiere confirmación.

Los QR de SaleToAcquirerData son independientes de los QR de credenciales. Deben contener el objeto JSON plano de SaleToAcquirerData, se escanean desde `Checkout`, y TapToPlay los codifica en Base64 dentro de `PaymentRequest.SaleData.SaleToAcquirerData`.
