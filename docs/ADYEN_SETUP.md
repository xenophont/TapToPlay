# Adyen Setup

## Payments App Flow

TapToPlay uses Adyen's Android Payments app links:

- Test boarded check: `https://www.adyen.com/test/boarded`
- Test board/reboard: `https://www.adyen.com/test/board`
- Test payment: `https://www.adyen.com/test/nexo`
- Live boarded check: `https://www.adyen.com/boarded`
- Live board/reboard: `https://www.adyen.com/board`
- Live payment: `https://www.adyen.com/nexo`

The return URL is:

```text
taptoplay://adyen-return
```

## Profile Selection

Credentials can come from either:

- `local.properties`, for local developer bootstrap.
- A scanned QR profile, stored in encrypted shared preferences.

Switching test/live is deliberate: scan profiles, then select the active one in the payment profile panel. Test profiles use Adyen test URLs and Management API. Live profiles use live URLs and Management API.

## Boarding

1. Select the target profile.
2. Tap `Check`.
3. If the Payments app is not boarded, it returns `installationId` and `boardingRequestToken`.
4. Tap `Board`.
5. TapToPlay calls the Adyen Management API `generatePaymentsAppBoardingToken` endpoint with the `boardingRequestToken`.
6. TapToPlay opens the Adyen Payments app `board` link with the Base64URL-encoded `boardingToken`.
7. The Adyen app returns `boarded=true` and an `installationId` to `taptoplay://adyen-return`.

Use `Reboard` to open `boarded?reboard=true`, receive a fresh `boardingRequestToken`, and then tap `Board` to finish setup for the newly selected merchant/store context.

## Live Checklist

- Use a live profile QR with `environment: "live"`.
- Verify the selected profile chip is live before checkout.
- Confirm the Adyen Payments app is live-capable and properly boarded.
- Do not ship this in-app secret model to production users.
- Replace demo `request` encoding with the Adyen-required encrypted Terminal API payload before real live payment use.

## Troubleshooting

- If QR scan fails, validate the payload against `docs/QR_CREDENTIALS.md`.
- If boarding fails, check the API key, merchant account, store ID, and Adyen API permissions.
- If the Adyen app does not open, confirm the Payments Test app is installed for test flows.
- If the return dialog does not appear, confirm the return URL is `taptoplay://adyen-return`.
