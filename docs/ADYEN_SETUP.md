# Configuracion de Adyen

## Flujo de la App Payments

TapToPlay usa los app links Android de Adyen Payments:

- Comprobacion de boarding en test: `https://www.adyen.com/test/boarded`
- Board/reboard en test: `https://www.adyen.com/test/board`
- Pago en test: `https://www.adyen.com/test/nexo`
- Comprobacion de boarding en live: `https://www.adyen.com/boarded`
- Board/reboard en live: `https://www.adyen.com/board`
- Pago en live: `https://www.adyen.com/nexo`

La return URL es:

```text
taptoplay://adyen-return
```

## Seleccion de Perfil

Las credenciales pueden venir de:

- `local.properties`, para bootstrap local de desarrollo.
- Un perfil QR escaneado, guardado en shared preferences cifradas.

El cambio test/live es deliberado: escanea perfiles y despues selecciona el perfil activo en el panel de payment profile. Los perfiles test usan URLs test de Adyen y Management API test. Los perfiles live usan URLs live y Management API live.

## Boarding

1. Selecciona el perfil objetivo.
2. Toca `Check`.
3. Si la app Payments no esta boarded, devuelve `installationId` y `boardingRequestToken`.
4. Toca `Board`.
5. TapToPlay llama al endpoint `generatePaymentsAppBoardingToken` de Adyen Management API con el `boardingRequestToken`.
6. TapToPlay abre el link `board` de la app Adyen Payments con el `boardingToken` codificado en Base64URL.
7. La app de Adyen devuelve `boarded=true` y un `installationId` a `taptoplay://adyen-return`.

Usa `Reboard` para abrir `boarded?reboard=true`, recibir un `boardingRequestToken` nuevo y despues tocar `Board` para completar la configuracion del nuevo contexto merchant/store seleccionado.

## Checklist Live

- Usa un QR de perfil live con `environment: "live"`.
- Verifica que el chip del perfil seleccionado sea live antes del checkout.
- Confirma que la app Adyen Payments sea compatible con live y este correctamente boarded.
- No envies este modelo con secretos dentro de la app a usuarios de produccion.
- Confirma que se envia un payload `request` cifrado real. TapToPlay usa `NexoCrypto` para crear un sobre cifrado `NexoBlob` y codificarlo en Base64URL para el App Link.
- No envies el modelo actual de credenciales in-app a usuarios de produccion.

## Solucion de Problemas

- Si el escaneo QR falla, valida el payload contra `docs/QR_CREDENTIALS.md`.
- Si el boarding falla, revisa la API key, merchant account, store ID y permisos de API de Adyen.
- Si la app de Adyen no se abre, confirma que la app Payments Test este instalada para flujos test.
- Si no aparece el dialogo de retorno, confirma que la return URL sea `taptoplay://adyen-return`.
