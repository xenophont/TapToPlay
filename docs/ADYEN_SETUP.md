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
- Confirm a real encrypted `request` payload is being sent. TapToPlay uses `NexoCrypto` to create an encrypted `NexoBlob` envelope and Base64URL-encode it for the App Link.
- Do not ship the current in-app credential model to production users.

## Troubleshooting

- If QR scan fails, validate the payload against `docs/QR_CREDENTIALS.md`.
- If boarding fails, check the API key, merchant account, store ID, and Adyen API permissions.
- If the Adyen app does not open, confirm the Payments Test app is installed for test flows.
- If the return dialog does not appear, confirm the return URL is `taptoplay://adyen-return`.

---

## Español

# Configuración de Adyen

## Flujo de la App Payments

TapToPlay usa los app links Android de Adyen Payments:

- Comprobación de boarding en test: `https://www.adyen.com/test/boarded`
- Board/reboard en test: `https://www.adyen.com/test/board`
- Pago en test: `https://www.adyen.com/test/nexo`
- Comprobación de boarding en live: `https://www.adyen.com/boarded`
- Board/reboard en live: `https://www.adyen.com/board`
- Pago en live: `https://www.adyen.com/nexo`

La return URL es:

```text
taptoplay://adyen-return
```

## Selección de Perfil

Las credenciales pueden venir de:

- `local.properties`, para bootstrap local de desarrollo.
- Un perfil QR escaneado, guardado en shared preferences cifradas.

El cambio test/live es deliberado: escanea perfiles y después selecciona el perfil activo en el panel de payment profile. Los perfiles test usan URLs test de Adyen y Management API test. Los perfiles live usan URLs live y Management API live.

## Boarding

1. Selecciona el perfil objetivo.
2. Toca `Check`.
3. Si la app Payments no está boarded, devuelve `installationId` y `boardingRequestToken`.
4. Toca `Board`.
5. TapToPlay llama al endpoint `generatePaymentsAppBoardingToken` de Adyen Management API con el `boardingRequestToken`.
6. TapToPlay abre el link `board` de la app Adyen Payments con el `boardingToken` codificado en Base64URL.
7. La app de Adyen devuelve `boarded=true` y un `installationId` a `taptoplay://adyen-return`.

Usa `Reboard` para abrir `boarded?reboard=true`, recibir un `boardingRequestToken` nuevo y después tocar `Board` para completar la configuración del nuevo contexto merchant/store seleccionado.

## Checklist Live

- Usa un QR de perfil live con `environment: "live"`.
- Verifica que el chip del perfil seleccionado sea live antes del checkout.
- Confirma que la app Adyen Payments sea compatible con live y esté correctamente boarded.
- No envíes este modelo con secretos dentro de la app a usuarios de producción.
- Confirma que se envía un payload `request` cifrado real. TapToPlay usa `NexoCrypto` para crear un sobre cifrado `NexoBlob` y codificarlo en Base64URL para el App Link.
- No envíes el modelo actual de credenciales in-app a usuarios de producción.

## Solución de Problemas

- Si el escaneo QR falla, valida el payload contra `docs/QR_CREDENTIALS.md`.
- Si el boarding falla, revisa la API key, merchant account, store ID y permisos de API de Adyen.
- Si la app de Adyen no se abre, confirma que la app Payments Test esté instalada para flujos test.
- Si no aparece el diálogo de retorno, confirma que la return URL sea `taptoplay://adyen-return`.
