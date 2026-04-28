# TapToPlay

TapToPlay is a Kotlin/Compose retail demo for a premium clothing store using Adyen's Android Payments app for Tap to Pay. It includes a boutique catalog, cart checkout, Adyen board/reboard links, payment launch links, QR-based credential profile import, and encrypted local profile storage.

## Demo Security Notice

Adyen recommends creating session and boarding requests from a backend. This project intentionally supports in-app credential profiles so the demo is easy to run from one Android device. That keeps secrets out of git, but it does not make secrets safe inside a distributed APK.

Use this architecture for demos only. A production app should move API keys, boarding-token calls, and Terminal API encryption material to a backend or a production-grade secure integration.

## Local Setup

1. Open the project in Android Studio.
2. Install the Adyen Payments Test app on a compatible Android device.
3. Add optional bootstrap credentials to `local.properties`.
4. Run the debug app on device.
5. Scan a QR credential profile or use the local bootstrap profile.
6. Tap `Check` so the Adyen Payments app returns boarding status.
7. If the app is not boarded, tap `Board` to exchange the returned boarding request token and finish setup.
8. Add catalog items and start checkout.

Example `local.properties` keys:

```properties
ADYEN_ENVIRONMENT=test
ADYEN_PROFILE_NAME=Demo Store TEST
ADYEN_MERCHANT_ID=YourMerchantAccount
ADYEN_STORE_ID=ST322LJ223223K5F
ADYEN_API_KEY=AQE...
ADYEN_CLIENT_KEY=test_...
ADYEN_TERMINAL_KEY_IDENTIFIER=CryptoKeyIdentifier
ADYEN_TERMINAL_KEY_VERSION=1
ADYEN_TERMINAL_PASSPHRASE=shared-key-passphrase
ADYEN_CURRENCY=EUR
ADYEN_COUNTRY_CODE=ES
```

`local.properties` is gitignored. Do not put real credentials into tracked files.

## Useful Commands

```powershell
.\gradlew test
.\gradlew assembleDebug
```

## Create a Credential QR Code

TapToPlay imports Adyen profiles by scanning a QR code that contains raw JSON. Use one QR per environment, for example one for test and one for live.

Example test payload:

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

To create the QR:

1. Fill the JSON with your Adyen test or live values.
2. Generate a QR code from the raw JSON text using a trusted offline QR tool, or an internal tool you control.
3. Treat the QR image like a password because it contains API keys and terminal encryption material.
4. Install and open TapToPlay, tap `Scan QR`, scan the profile, and select it in the payment profile panel.

Do not commit QR images or JSON files with real credentials. For more detail and a live example shape, see `docs/QR_CREDENTIALS.md`.

## Create a SaleToAcquirerData QR Code

TapToPlay can also scan a QR code to replace the `SaleToAcquirerData` object for the next payment requests. This is useful for testing Adyen features that are controlled through `SaleToAcquirerData`.

The recommended QR contains the plain Adyen `SaleToAcquirerData` JSON object. TapToPlay Base64-encodes exactly that object and writes it into `PaymentRequest.SaleData.SaleToAcquirerData`.

Example payload:

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
  "tenderOption": "ReceiptHandler,AskGratuity",
  "additionalData": {
    "authorisationType": "PreAuth",
    "manualCapture": "false",
    "taxfree.indicator": false
  }
}
```

To use it:

1. Generate a QR code from the raw JSON above, replacing properties with the values you want to test.
2. Open TapToPlay and add products to the cart.
3. In `Checkout`, tap `Scan data QR`.
4. Confirm the checkout panel shows `Scanned SaleToAcquirerData`.
5. Start payment. The encrypted Terminal API request will include your structured JSON as Base64 in `SaleToAcquirerData`.

Use `Reset` in the checkout panel to go back to the default retail demo metadata.

Older TapToPlay QR payloads with `schema`, `displayName`, and `saleToAcquirerData` are still accepted, but newly generated QRs should use the plain object above to avoid sending wrapper fields or demo defaults to Adyen.

## SaleToAcquirerData Favorites

For demos, you can save frequently used `SaleToAcquirerData` setups as favorites:

1. Load a setup with `Scan data QR`, or open `View` and edit fields manually.
2. Tap `Save` in the checkout `SaleToAcquirerData` panel or inside the field editor.
3. Use the `Favorites` list in checkout to tap `Use` before starting a transaction.
4. Tap `Remove` on a favorite when you no longer need that preset.

Favorites are stored locally with encrypted Android preferences. They are demo presets for faster testing; do not store sensitive production-only data in demo APKs.

## Build and Deploy

From Android Studio:

1. Open this project folder.
2. Let Gradle sync complete.
3. Select the `app` run configuration.
4. Connect a compatible Android device with NFC.
5. Press Run.

From PowerShell:

```powershell
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

For test payments, install the Adyen Payments Test app on the same device. For live payments, install the live Adyen Payments app, scan a live QR profile, explicitly select it, then run `Check`, `Board`, and checkout.

## Current Payment Boundary

The app builds real Adyen app links for test/live environments and performs the Management API boarding-token call from the demo app. Terminal API requests are wrapped by `adyen/NexoCrypto.kt` using the Adyen local-communications protection shape: encrypted `NexoBlob`, `SecurityTrailer`, and Base64URL encoding for the `request` App Link parameter.

This is still a demo security model because credentials live on-device. For production, move credential storage and token/session work to a backend or hardened secure component.

## More Docs

- `docs/ADYEN_SETUP.md`: boarding, reboarding, test/live setup, and troubleshooting.
- `docs/QR_CREDENTIALS.md`: QR JSON schema and examples.
- `AGENTS.md`: implementation guidance for future coding agents.
