# TapToPlay

TapToPlay is a Kotlin/Compose premium retail POS demo for Adyen Tap to Pay on Android. It presents a boutique clothing catalog first, then adds an Adyen operations console for checkout, Payments App boarding, app-instance management, transaction inspection, diagnostics, SaleToAcquirerData testing, and referenced refunds. Personal project. Not affiliated with or endorsed by my employer.

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
.\gradlew bundleRelease
```

## Google Play Testing

The Play application ID is `com.xenophont.taptoplay`. Confirm that this is final
before the first Play Console upload because Google Play package names cannot be
changed or reused later.

Release builds read signing settings from local-only properties and continue to
receive blank Adyen bootstrap values. Testers should scan credential QR profiles
on device. Increment `tapToPlayVersionCode` in `gradle.properties` before every
new Play upload.

Use `docs/PRIVACY_POLICY_DRAFT.md` as a starting point before closed, open, or
production release.

## Credential QR Codes

TapToPlay imports Adyen profiles by scanning a QR code that contains raw JSON. Use one QR per environment, for example one for test and one for live.

For merchant-scoped profiles without `storeId`, TapToPlay shows `merchantId` as the primary profile label. For store-scoped profiles, `displayName` is a fallback label: after scanning a QR with `storeId`, TapToPlay calls the Adyen Management API v3 stores endpoint with the scanned API key and merchant account, resolves the matching store `reference`, and shows that value as the primary profile label.

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
- `Board`: calls the Adyen Management API `generatePaymentsAppBoardingToken` endpoint with the returned `boardingRequestToken`, then opens the `board` App Link with the generated `boardingToken`. This backend-style call stays in the app for demo purposes.
- `Reboard`: opens `boarded?reboard=true`, then uses `Board` after Adyen returns a fresh request token.
- `Refresh`: calls the Payments App API to list Payments App instances for the selected merchant or store.
- `Revoke instance`: revokes a listed app instance after an explicit confirmation.
- `Remove`: removes the local encrypted profile and its saved local boarding state. This does not revoke the Adyen Payments App instance by itself.

The Management API calls use the selected scanned/bootstrap profile API key. Make sure the API credential has the required Adyen roles for store-name lookup when using `storeId`, boarding, listing, and revoking Payments App instances.

## Transactions, Responses, and Refunds

Each payment or refund attempt is saved in the `Transactions` tab. Tap `Inspect` to review:

- The structured Terminal API request, including `ServiceID`, message category, Merchant Reference, and decoded SaleToAcquirerData.
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

TapToPlay es una demo de TPV retail premium en Kotlin/Compose para Adyen Tap to Pay en Android. Presenta primero un catálogo de ropa boutique y después añade una consola operativa de Adyen para checkout, boarding de Payments App, gestión de instancias de la app, inspección de transacciones, diagnósticos, pruebas de SaleToAcquirerData y reembolsos referenciados.Proyecto personal. No afiliado ni respaldado por mi empleador.

## Aviso de seguridad de la demo

Adyen recomienda crear las solicitudes de sesión, boarding y pago desde un backend. Este proyecto admite intencionadamente perfiles de credenciales dentro de la app para que la demo pueda ejecutarse desde un único dispositivo Android. Eso mantiene los secretos fuera de git, pero no hace que los secretos sean seguros dentro de un APK distribuido.

Usa esta arquitectura solo para demos. Una app de producción debería mover las claves API, las llamadas de tokens de boarding, el material de cifrado de Terminal API y los permisos operativos de revocación/listado a un backend o a otro componente seguro de nivel productivo.

TapToPlay refuerza el modelo de demo donde resulta práctico:

- `local.properties` está ignorado por git y las builds release reciben valores de arranque de Adyen en blanco.
- Los perfiles escaneados, el estado de boarding, los registros de transacciones y los favoritos de SaleToAcquirerData se guardan con preferencias cifradas de Android.
- La copia de seguridad de Android está desactivada, y los archivos sensibles de preferencias cifradas también se excluyen de las reglas de backup y transferencia de dispositivo.
- Los secretos se muestran enmascarados en la UI.
- Los pagos live requieren un diálogo de confirmación por cada cobro.
- Los enlaces de retorno cortos como `result=success` no se tratan como pagos aprobados; los estados aprobado/rechazado requieren un payload completo de respuesta de Terminal API.

## Configuración local

1. Abre el proyecto en Android Studio.
2. Instala la app Adyen Payments Test en un dispositivo Android compatible.
3. Opcionalmente, añade credenciales de arranque solo para debug en `local.properties`.
4. Ejecuta la app debug en el dispositivo.
5. Abre `Payments App`, escanea un perfil QR de credenciales o usa el perfil local de arranque.
6. Toca `Check` para que la app Adyen Payments devuelva el estado de boarding.
7. Si la app no está boarded, toca `Board` para intercambiar el token de solicitud de boarding devuelto y terminar la configuración.
8. Abre `Catalog`, añade productos y después abre `Checkout` para lanzar un pago.

Ejemplo de claves de `local.properties`:

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

`local.properties` está ignorado por git. No pongas credenciales reales en archivos versionados.

## Pestañas de la app

- `Catalog`: la primera superficie retail usable. Añade prendas boutique al carrito.
- `Checkout`: totales del carrito, lanzamiento de pagos test/live, escaneo QR de SaleToAcquirerData, favoritos y edición de campos.
- `Payments App`: escaneo QR de credenciales, selección del perfil activo, check/board/reboard, eliminación de perfiles, consulta de instancias de Payments App y revocación protegida.
- `Transactions`: intentos guardados de pago/reembolso con solicitud, respuesta, recibo, AdditionalResponse e inspección de reembolsos.
- `Diagnostics`: resúmenes redactados del perfil, boarding, SaleToAcquirerData, API de Payments App y estado de transacciones.

## Comandos útiles

```powershell
.\gradlew test
.\gradlew assembleDebug
.\gradlew bundleRelease
```

## Pruebas en Google Play

El ID de aplicación de Play es `com.xenophont.taptoplay`. Confirma que es definitivo antes de la primera subida a Play Console, porque los nombres de paquete de Google Play no se pueden cambiar ni reutilizar después.

Las builds release leen la configuración de firma desde propiedades solo locales y siguen recibiendo valores de arranque de Adyen en blanco. Los testers deberían escanear perfiles QR de credenciales en el dispositivo. Incrementa `tapToPlayVersionCode` en `gradle.properties` antes de cada nueva subida a Play.

Usa `docs/PRIVACY_POLICY_DRAFT.md` como punto de partida antes de una release cerrada, abierta o de producción.

## Códigos QR de credenciales

TapToPlay importa perfiles de Adyen escaneando un código QR que contiene JSON sin procesar. Usa un QR por entorno, por ejemplo uno para test y otro para live.

Para perfiles de ámbito merchant sin `storeId`, TapToPlay muestra `merchantId` como etiqueta principal del perfil. Para perfiles de ámbito tienda, `displayName` es una etiqueta de fallback: después de escanear un QR con `storeId`, TapToPlay llama al endpoint de tiendas de Adyen Management API v3 con la clave API escaneada y la cuenta merchant, resuelve el `reference` de la tienda correspondiente y muestra ese valor como etiqueta principal del perfil.

Payload de ejemplo para test:

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

Para crear el QR:

1. Rellena el JSON con tus valores de Adyen test o live.
2. Genera un código QR a partir del texto JSON sin procesar usando una herramienta QR offline de confianza o una herramienta interna que controles.
3. Trata la imagen QR como una contraseña, porque contiene claves API y material de cifrado de terminal.
4. Abre `Payments App`, toca `Scan QR`, escanea el perfil y selecciónalo deliberadamente.

No comitees imágenes QR ni archivos JSON con credenciales reales. Para más detalle y un ejemplo de estructura live, consulta `docs/QR_CREDENTIALS.md`.

## Códigos QR de SaleToAcquirerData

TapToPlay puede escanear un código QR para reemplazar el objeto `SaleToAcquirerData` de las siguientes solicitudes de pago. Esto es útil para probar funcionalidades de Adyen controladas mediante `SaleToAcquirerData`.

El QR recomendado contiene el objeto JSON plano de Adyen `SaleToAcquirerData`. TapToPlay codifica exactamente ese objeto en Base64 y lo escribe en `PaymentRequest.SaleData.SaleToAcquirerData`.

Payload de ejemplo:

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

Usa `Scan data QR` en `Checkout`, `View` para inspeccionar/editar campos, `Save` para favoritos y `Reset` para volver a los metadatos retail de demo por defecto. Los códigos QR de SaleToAcquirerData deben contener el objeto SaleToAcquirerData plano; los payloads envoltorio de TapToPlay ya no están soportados.

## Operaciones de Payments App

La pestaña `Payments App` admite:

- `Check`: abre el App Link documentado `boarded` y parsea el estado de boarding devuelto y el `data` de retorno decodificado.
- `Board`: llama al endpoint `generatePaymentsAppBoardingToken` de Adyen Management API con el `boardingRequestToken` devuelto y después abre el App Link `board` con el `boardingToken` generado. Esta llamada de estilo backend se mantiene dentro de la app con fines de demo.
- `Reboard`: abre `boarded?reboard=true` y después usa `Board` cuando Adyen devuelve un token de solicitud nuevo.
- `Refresh`: llama a la Payments App API para listar instancias de Payments App para el merchant o tienda seleccionados.
- `Revoke instance`: revoca una instancia listada de la app después de una confirmación explícita.
- `Remove`: elimina el perfil local cifrado y su estado local de boarding guardado. Esto no revoca por sí solo la instancia de Adyen Payments App.

Las llamadas a Management API usan la clave API del perfil escaneado/de arranque seleccionado. Asegúrate de que la credencial API tenga los roles requeridos de Adyen para consultar nombres de tienda al usar `storeId`, hacer boarding, listar y revocar instancias de Payments App.

## Transacciones, respuestas y reembolsos

Cada intento de pago o reembolso se guarda en la pestaña `Transactions`. Toca `Inspect` para revisar:

- La solicitud estructurada de Terminal API, incluyendo `ServiceID`, categoría de mensaje, Merchant Reference y SaleToAcquirerData decodificado.
- La respuesta decodificada de Terminal API cuando esté disponible.
- Una vista legible de campos de respuesta con los valores clave arriba.
- Valores decodificados de `AdditionalResponse`.
- Datos de recibo generados por Adyen cuando se devuelvan.
- La URI de retorno sin procesar y el JSON raw detrás de acciones intencionadas de inspección.

Si una respuesta de pago aprobado incluye un identificador de transacción de Terminal API, el inspector de transacciones habilita `Refund`. Esto lanza un reembolso referenciado usando un `ReversalRequest` contra la transacción original.

## Compilación y despliegue

Desde Android Studio:

1. Abre esta carpeta de proyecto.
2. Deja que Gradle termine la sincronización.
3. Selecciona la configuración de ejecución `app`.
4. Conecta un dispositivo Android compatible con NFC.
5. Pulsa Run.

Desde PowerShell:

```powershell
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Para pagos test, instala la app Adyen Payments Test en el mismo dispositivo. Para pagos live, instala la app Adyen Payments live, escanea un perfil QR live, selecciónalo explícitamente, ejecuta `Check` y `Board`, y después confirma cada cobro live en checkout.

## Límite actual de pagos

La app construye App Links reales de Adyen para entornos test/live y realiza llamadas a Management API desde la app de demo. La construcción de payloads de Terminal API vive en el paquete `adyen`, y el cifrado permanece aislado en `adyen/NexoCrypto.kt`. Los App Links de pago usan el parámetro documentado `request` con un sobre Nexo cifrado y codificado en Base64URL.

Este sigue siendo un modelo de seguridad de demo porque las credenciales viven en el dispositivo. Para producción, mueve el almacenamiento de credenciales y el trabajo de tokens/sesiones a un backend o a un componente seguro reforzado.

## Más documentación

- `docs/ADYEN_SETUP.md`: boarding, reboarding, listado/revocación de Payments App, configuración test/live y troubleshooting.
- `docs/QR_CREDENTIALS.md`: esquema JSON de QR, ejemplos, límites y gestión de seguridad.
