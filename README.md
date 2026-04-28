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

## Current Payment Boundary

The app builds real Adyen app links for test/live environments and performs the Management API boarding-token call from the demo app. Terminal API requests are wrapped by `adyen/NexoCrypto.kt` using the Adyen local-communications protection shape: encrypted `NexoBlob`, `SecurityTrailer`, and Base64URL encoding for the `request` App Link parameter.

This is still a demo security model because credentials live on-device. For production, move credential storage and token/session work to a backend or hardened secure component.

## More Docs

- `docs/ADYEN_SETUP.md`: boarding, reboarding, test/live setup, and troubleshooting.
- `docs/QR_CREDENTIALS.md`: QR JSON schema and examples.
- `AGENTS.md`: implementation guidance for future coding agents.
