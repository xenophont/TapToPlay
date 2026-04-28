# TapToPlay Agent Guidance

## Product Direction

Keep TapToPlay as a premium boutique retail POS demo. The first screen should be the usable catalog and checkout experience, not a landing page. Prefer restrained colors, polished typography, clear checkout controls, and realistic clothing placeholder data.

## Security Rules

- Never commit real Adyen credentials.
- Keep `local.properties` and credential QR payloads out of git.
- Mask secrets in UI.
- Preserve encrypted profile storage for scanned credentials.
- Keep live payment switching deliberate through the profile selector.
- Keep README warnings that in-app credentials are demo-only.

## Adyen Rules

Use official Adyen docs as source of truth for Android Payments app links, Management API boarding tokens, and Terminal API payload/encryption behavior. Test and live environments must stay visibly distinct in the UI and code.

The current `TerminalPaymentRequestBuilder` and `NexoCrypto` boundary exists so Terminal API payload construction and encryption stay isolated from catalog/cart UI. The payment App Link must use the documented `request` query parameter, and boarding must keep the documented check-token-finish sequence. Do not spread payment payload construction or crypto code across Compose code.

## Implementation Rules

- Keep domain code in `catalog`, `cart`, `profiles`, and `adyen`.
- Keep UI in Compose and avoid adding XML screens.
- Add or update unit tests for cart math, QR validation, profile switching, link construction, and return parsing when behavior changes.
- Do not add real product images until the user provides them.
