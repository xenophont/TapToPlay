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
  "saleToAcquirerData": {
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
    "recurringProcessingModel": "Subscription",
    "recurringDetailName": "YOUR_VALUE",
    "recurringTokenService": "YOUR_VALUE",
    "shopperEmail": "S.Hopper@example.com",
    "shopperReference": "YOUR_UNIQUE_SHOPPER_ID",
    "shopperStatement": "YOUR_PAYMENT_DESCRIPTION",
    "store": "YOUR_STORE_REFERENCE",
    "tenderOption": "ReceiptHandler,AskGratuity",
    "additionalData": {
      "authorisationType": "PreAuth",
      "lodging.customerServiceTollFreeNumber": "1800433999",
      "lodging.checkInDate": "20200219",
      "lodging.checkOutDate": "20200222",
      "lodging.folioNumber": "13579111315",
      "lodging.propertyPhoneNumber": "1800433999",
      "lodging.room1.rate": "15000",
      "lodging.room1.tax": "1000",
      "lodging.room1.numberOfNights": "3",
      "lodging.fireSafetyActIndicator": "Y",
      "lodging.totalRoomTax": "2000",
      "split.api": "1",
      "split.nrOfItems": "2",
      "split.totalAmount": "62000",
      "split.currencyCode": "EUR",
      "split.item1.amount": "60000",
      "split.item1.type": "BalanceAccount",
      "split.item1.account": "BA00000000000000000000001",
      "split.item1.reference": "TestPayment",
      "split.item1.description": "TestDescription",
      "split.item2.amount": "2000",
      "split.item2.type": "Commission",
      "split.item2.reference": "TestCommission",
      "surchargeFee": "VALUE_IN_MINOR_UNITS",
      "taxfree.indicator": false,
      "travelEntertainmentAuthData.market": "H",
      "travelEntertainmentAuthData.duration": "3",
      "manualCapture": "false"
    }
  }
}
```

Rules:

- `schema` must be `taptoplay.adyen.saleToAcquirerData.v1`.
- `displayName` is shown in the checkout panel.
- `saleToAcquirerData` must be a non-empty JSON object. Legacy QR codes with `properties` are still accepted.
- Values may be strings, numbers, booleans, arrays, or nested objects.
- TapToPlay deep-merges this object over default retail demo data, serializes the result as JSON, Base64-encodes it, and sends it as `PaymentRequest.SaleData.SaleToAcquirerData`.
