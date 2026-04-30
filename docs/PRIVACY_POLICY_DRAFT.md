# TapToPlay Privacy Policy Draft

Last updated: April 30, 2026

This policy is a starting point for Google Play testing and review. Publish it
at a public, non-editable URL before using it in Play Console.

## Developer

Developer: Javier de No

Contact: javier.deno@gmail.com

App package: `com.xenophont.taptoplay`

## What TapToPlay Is

TapToPlay is a demo retail point-of-sale app for testing Adyen Tap to Pay on
Android flows. It lets testers build a local cart, scan Adyen profile QR codes,
board the Adyen Payments app, launch test or live payments, inspect payment
results, and test referenced refunds.

TapToPlay is a demo app. Do not use production credentials unless you are
authorized to do so and understand that in-app credentials are not a production
security model.

## Data Processed on the Device

TapToPlay may store the following data locally on the Android device:

- Cart contents and demo transaction records.
- App language and display preferences.
- Scanned Adyen profile values, including merchant/store identifiers, API keys,
  client keys, and Terminal API encryption settings.
- Boarding status and Payments App instance metadata returned by Adyen APIs.
- SaleToAcquirerData test values and favorites.
- Payment and refund request/response details needed for demo diagnostics.

Secrets are masked in the app UI where practical. Sensitive local app data is
stored using encrypted Android preferences, and Android backup is disabled.

## Camera

TapToPlay uses the camera only when a tester chooses to scan a QR code. QR codes
are used to import Adyen profile settings or SaleToAcquirerData test values.
TapToPlay does not store photos or video from the camera.

## Internet and Adyen

TapToPlay uses internet access to communicate with Adyen services and to launch
or coordinate flows with the Adyen Payments app. When testers board the Payments
app, list app instances, create payment requests, inspect payment results, or
run referenced refunds, relevant request data may be sent to Adyen.

Adyen handles data according to Adyen's own terms and privacy documentation.

## Sharing

TapToPlay does not sell personal data. TapToPlay does not include advertising or
analytics SDKs.

Data may be shared with Adyen when testers use Adyen payment, boarding,
management, or refund flows. Data may also be processed by Google Play and the
Android operating system as part of normal app distribution, installation, and
runtime behavior.

## Retention and Deletion

TapToPlay keeps local demo data until the tester removes profiles or records in
the app, clears app storage, or uninstalls the app. For questions or deletion
requests related to any developer-held support records, contact
javier.deno@gmail.com.

## Security

TapToPlay uses encrypted local storage for scanned profiles and related demo
state, disables Android backup, and avoids embedding Adyen bootstrap credentials
in release builds. This reduces accidental exposure during demos, but it does
not make in-app API credentials suitable for production use.

## Changes

This policy may be updated as TapToPlay changes. The latest version should be
published at the privacy policy URL used in Google Play Console, with the
updated date shown above.
