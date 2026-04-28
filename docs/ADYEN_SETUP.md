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
2. Tap `Board`.
3. TapToPlay calls the Adyen Management API for a boarding token.
4. TapToPlay opens the Adyen Payments app board link.
5. The Adyen app returns to `taptoplay://adyen-return`.

Use `Reboard` to force a new boarding flow when changing merchant/store context.

## Live Checklist

- Use a live profile QR with `environment: "live"`.
- Verify the selected profile chip is live before checkout.
- Confirm the Adyen Payments app is live-capable and properly boarded.
- Do not ship this in-app secret model to production users.
- Replace demo `nexoBlob` encoding with the Adyen-required encrypted Terminal API payload before real live payment use.

## Troubleshooting

- If QR scan fails, validate the payload against `docs/QR_CREDENTIALS.md`.
- If boarding fails, check the API key, merchant account, store ID, and Adyen API permissions.
- If the Adyen app does not open, confirm the Payments Test app is installed for test flows.
- If the return dialog does not appear, confirm the return URL is `taptoplay://adyen-return`.
