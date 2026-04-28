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

Credential QR codes use `taptoplay.adyen.profile.v1`. SaleToAcquirerData QR codes use a separate schema so they can be scanned from checkout without changing the active payment profile.

```json
{
  "schema": "taptoplay.adyen.saleToAcquirerData.v1",
  "displayName": "Preauth experiment",
  "properties": {
    "authorisationType": "PreAuth",
    "metadata.experiment": "qr-sale-to-acquirer-data",
    "metadata.operator": "demo-user"
  }
}
```

Rules:

- `schema` must be `taptoplay.adyen.saleToAcquirerData.v1`.
- `displayName` is shown in the checkout panel.
- `properties` must be a non-empty JSON object.
- Values may be strings, numbers, booleans, arrays, or nested objects.
- TapToPlay merges these properties over the default retail demo metadata, serializes the result as JSON, Base64-encodes it, and sends it as `PaymentRequest.SaleData.SaleToAcquirerData`.
