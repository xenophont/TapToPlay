# TapToPlay

TapToPlay is a Kotlin/Compose premium retail POS demo for Adyen Tap to Pay on Android. It presents a boutique clothing catalog first, then adds an Adyen operations console for checkout, Payments App boarding, app-instance management, transaction inspection, diagnostics, SaleToAcquirerData testing, and referenced refunds.

## Demo Security Notice

Adyen recommends creating session, boarding, and payment requests from a backend. This project intentionally supports in-app credential profiles so the demo can run from one Android device. That keeps secrets out of git, but it does not make secrets safe inside a distributed APK.

Use this architecture for demos only. A production app should move API keys, boarding-token calls, Terminal API encryption material, and operational revoke/list permissions to a backend or another production-grade secure component.

TapToPlay hardens the demo model where practical:

- `local.properties` is gitignored and release builds receive blank Adyen bootstrap values.
- Scanned profiles, boarding state, transaction records, and SaleToAcquirerData favorites are stored with encrypted Android preferences.
- Android backup is disabled, and sensitive encrypted preference files are also excluded from backup and device transfer rules.
- Secrets are masked in the UI.
- Live payments require a per-charge confirmation dialog.
- Short return links such as `result=success` are not treated as approved payments; approved/refused states require a full Terminal API response payload.

## Local Setup

1. Open the project in Android Studio.
2. Install the Adyen Payments Test app on a compatible Android device.
3. Optionally add debug-only bootstrap credentials to `local.properties`.
4. Run the debug app on device.
5. Open `Payments App`, scan a credential QR profile, or use the local bootstrap profile.
6. Tap `Check` so the Adyen Payments app returns boarding status.
7. If the app is not boarded, tap `Board` to exchange the returned boarding request token and finish setup.
8. Open `Catalog`, add products, then open `Checkout` to launch a payment.

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

## App Tabs

- `Catalog`: the first usable retail surface. Add boutique clothing items to the cart.
- `Checkout`: cart totals, test/live payment launch, SaleToAcquirerData QR scanning, favorites, and field editing.
- `Payments App`: credential QR scanning, active profile selection, check/board/reboard, profile removal, Payments App instance lookup, and guarded revoke.
- `Transactions`: saved payment/refund attempts with request, response, receipt, AdditionalResponse, and refund inspection.
- `Diagnostics`: redacted profile, boarding, SaleToAcquirerData, Payments App API, and transaction status summaries.

## Useful Commands

```powershell
.\gradlew test
.\gradlew assembleDebug
```

## Credential QR Codes

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
4. Open `Payments App`, tap `Scan QR`, scan the profile, and select it deliberately.

Do not commit QR images or JSON files with real credentials. For more detail and a live example shape, see `docs/QR_CREDENTIALS.md`.

## SaleToAcquirerData QR Codes

TapToPlay can scan a QR code to replace the `SaleToAcquirerData` object for the next payment requests. This is useful for testing Adyen features controlled through `SaleToAcquirerData`.

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
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION"
}
```

Use `Scan data QR` in `Checkout`, `View` to inspect/edit fields, `Save` for favorites, and `Reset` to return to the default retail demo metadata. SaleToAcquirerData QR codes must contain the plain SaleToAcquirerData object; TapToPlay wrapper payloads are no longer supported.

## Payments App Operations

The `Payments App` tab supports:

- `Check`: opens the documented `boarded` App Link and parses returned boarding status and decoded return `data`.
- `Board`: calls the Adyen Management API `generatePaymentsAppBoardingToken` endpoint with the returned `boardingRequestToken`, then opens the `board` App Link.
- `Reboard`: opens `boarded?reboard=true`, then uses `Board` after Adyen returns a fresh request token.
- `Refresh`: calls the Payments App API to list Payments App instances for the selected merchant or store.
- `Revoke instance`: revokes a listed app instance after an explicit confirmation.
- `Remove`: removes the local encrypted profile and its saved local boarding state. This does not revoke the Adyen Payments App instance by itself.

The Management API calls use the selected scanned/bootstrap profile API key. Make sure the API credential has the required Adyen roles for boarding, listing, and revoking Payments App instances.

## Transactions, Responses, and Refunds

Each payment or refund attempt is saved in the `Transactions` tab. Tap `Inspect` to review:

- The structured Terminal API request, including `ServiceID`, message category, sale transaction ID, and decoded SaleToAcquirerData.
- The decoded Terminal API response when available.
- A readable response field view with key values at the top.
- Decoded `AdditionalResponse` values.
- Adyen-generated receipt data when returned.
- The raw return URI and raw JSON behind intentional inspect actions.

If an approved payment response includes a Terminal API transaction identifier, the transaction inspector enables `Refund`. This launches a referenced refund using a `ReversalRequest` against the original transaction.

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

For test payments, install the Adyen Payments Test app on the same device. For live payments, install the live Adyen Payments app, scan a live QR profile, select it explicitly, run `Check` and `Board`, then confirm each live charge in checkout.

## Current Payment Boundary

The app builds real Adyen App Links for test/live environments and performs Management API calls from the demo app. Terminal API payload construction lives in the `adyen` package, and encryption stays isolated in `adyen/NexoCrypto.kt`. Payment App Links use the documented `request` query parameter with a Base64URL-encoded encrypted Nexo envelope.

This is still a demo security model because credentials live on-device. For production, move credential storage and token/session work to a backend or hardened secure component.

## More Docs

- `docs/ADYEN_SETUP.md`: boarding, reboarding, Payments App list/revoke, test/live setup, and troubleshooting.
- `docs/QR_CREDENTIALS.md`: QR JSON schema, examples, limits, and security handling.

---

## Español

TapToPlay es una demo POS retail en Kotlin/Compose para una tienda premium usando Adyen Tap to Pay en Android. Muestra primero el catálogo boutique y después una consola operativa de Adyen para checkout, boarding, gestión de instancias de Payments App, inspección de transacciones, diagnósticos, pruebas de SaleToAcquirerData y refunds referenciados.

Usa esta arquitectura solo para demos. Las credenciales se guardan cifradas en el dispositivo, los secretos se muestran enmascarados, los backups están desactivados, los builds release no reciben credenciales de `local.properties`, y cada pago live requiere confirmación explícita.

Flujo rápido:

1. Ejecuta la app debug.
2. En `Payments App`, escanea o selecciona un perfil.
3. Toca `Check` y después `Board` si hace falta.
4. En `Catalog`, añade productos.
5. En `Checkout`, lanza pagos test o confirma pagos live.
6. En `Transactions`, inspecciona requests, responses, recibos y refunds.
7. En `Diagnostics`, revisa el estado operativo en formato redactado.

No comitees QR, JSON ni archivos con credenciales reales. Para el detalle de boarding y QR, revisa `docs/ADYEN_SETUP.md` y `docs/QR_CREDENTIALS.md`.
