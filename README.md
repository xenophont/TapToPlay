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
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION"
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

## Transaction History, Responses, and Refunds

Each payment or refund attempt is saved in the `Transactions` panel. Tap `Inspect` to review:

- The raw Terminal API request sent to the Adyen Payments app.
- The raw return URI.
- The decoded Terminal API response when available.
- A readable field view with key response values at the top.
- Decoded `AdditionalResponse` data, including Base64 JSON or Base64 values inside key-value responses.

If an approved payment response includes a Terminal API transaction identifier, the transaction inspector enables `Refund`. This launches a referenced refund using a `ReversalRequest` against the original transaction. Refund outcomes can still require Adyen webhooks or Customer Area checks depending on your Adyen setup.

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

---

## Español

TapToPlay es una demo retail en Kotlin/Compose para una tienda premium de ropa usando la app Android Payments de Adyen para Tap to Pay. Incluye catálogo boutique, carrito, checkout, enlaces de boarding/reboarding de Adyen, lanzamiento de pagos, importación de perfiles por QR y almacenamiento local cifrado de perfiles.

## Aviso de Seguridad de la Demo

Adyen recomienda crear las solicitudes de sesión y boarding desde un backend. Este proyecto permite perfiles con credenciales dentro de la app para que la demo se pueda ejecutar fácilmente desde un único dispositivo Android. Eso mantiene los secretos fuera de git, pero no hace que los secretos sean seguros dentro de un APK distribuido.

Usa esta arquitectura solo para demos. Una app de producción debería mover las API keys, las llamadas de boarding token y el material de cifrado de Terminal API a un backend o a una integración segura de nivel producción.

## Configuración Local

1. Abre el proyecto en Android Studio.
2. Instala la app Adyen Payments Test en un dispositivo Android compatible.
3. Añade credenciales bootstrap opcionales en `local.properties`.
4. Ejecuta la app debug en el dispositivo.
5. Escanea un perfil de credenciales por QR o usa el perfil bootstrap local.
6. Toca `Check` para que la app Adyen Payments devuelva el estado de boarding.
7. Si la app no está boarded, toca `Board` para intercambiar el boarding request token devuelto y completar la configuración.
8. Añade productos del catálogo e inicia el checkout.

Ejemplo de claves para `local.properties`:

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

## Comandos Útiles

```powershell
.\gradlew test
.\gradlew assembleDebug
```

## Crear un QR de Credenciales

TapToPlay importa perfiles de Adyen escaneando un QR que contiene JSON en crudo. Usa un QR por entorno, por ejemplo uno para test y otro para live.

Ejemplo de payload test:

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

1. Rellena el JSON con tus valores test o live de Adyen.
2. Genera un QR desde el texto JSON en crudo usando una herramienta offline de confianza, o una herramienta interna que controles.
3. Trata la imagen QR como una contraseña porque contiene API keys y material de cifrado de terminal.
4. Instala y abre TapToPlay, toca `Scan QR`, escanea el perfil y selecciónalo en el panel de payment profile.

No comitees imágenes QR ni archivos JSON con credenciales reales. Para más detalle y un ejemplo live, revisa `docs/QR_CREDENTIALS.md`.

## Crear un QR de SaleToAcquirerData

TapToPlay también puede escanear un QR para reemplazar el objeto `SaleToAcquirerData` de las siguientes solicitudes de pago. Esto es útil para probar features de Adyen controladas mediante `SaleToAcquirerData`.

El QR recomendado contiene el objeto JSON plano de Adyen `SaleToAcquirerData`. TapToPlay codifica en Base64 exactamente ese objeto y lo escribe en `PaymentRequest.SaleData.SaleToAcquirerData`.

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

Para usarlo:

1. Genera un QR desde el JSON en crudo anterior, reemplazando las propiedades por los valores que quieras probar.
2. Abre TapToPlay y añade productos al carrito.
3. En `Checkout`, toca `Scan data QR`.
4. Confirma que el panel de checkout muestra `Scanned SaleToAcquirerData`.
5. Inicia el pago. La solicitud cifrada de Terminal API incluirá tu JSON estructurado como Base64 en `SaleToAcquirerData`.

Usa `Reset` en el panel de checkout para volver a los metadatos por defecto de la demo retail.

Los QR antiguos de TapToPlay con `schema`, `displayName` y `saleToAcquirerData` siguen siendo aceptados, pero los QR nuevos deberían usar el objeto plano anterior para evitar enviar campos wrapper o defaults de demo a Adyen.

## Favoritos de SaleToAcquirerData

Para demos, puedes guardar configuraciones frecuentes de `SaleToAcquirerData` como favoritos:

1. Carga una configuración con `Scan data QR`, o abre `View` y edita los campos manualmente.
2. Toca `Save` en el panel `SaleToAcquirerData` del checkout o dentro del editor de campos.
3. Usa la lista `Favorites` en checkout y toca `Use` antes de iniciar una transacción.
4. Toca `Remove` en un favorito cuando ya no necesites ese preset.

Los favoritos se guardan localmente con preferencias Android cifradas. Son presets de demo para acelerar pruebas; no guardes datos sensibles de producción en APKs de demo.

## Historial de Transacciones, Respuestas y Refunds

Cada intento de pago o refund se guarda en el panel `Transactions`. Toca `Inspect` para revisar:

- La solicitud Terminal API en crudo enviada a la app Adyen Payments.
- La URI de retorno en crudo.
- La respuesta Terminal API decodificada cuando exista.
- Una vista legible de campos con los valores clave de respuesta arriba.
- Datos `AdditionalResponse` decodificados, incluyendo JSON Base64 o valores Base64 dentro de respuestas key-value.

Si una respuesta de pago aprobada incluye un identificador de transacción de Terminal API, el inspector de transacciones habilita `Refund`. Esto lanza un refund referenciado usando una `ReversalRequest` contra la transacción original. El resultado del refund puede seguir requiriendo webhooks de Adyen o comprobaciones en Customer Area según tu configuración de Adyen.

## Build y Despliegue

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

Para pagos test, instala la app Adyen Payments Test en el mismo dispositivo. Para pagos live, instala la app live de Adyen Payments, escanea un perfil QR live, selecciónalo explícitamente y después ejecuta `Check`, `Board` y checkout.

## Límite Actual de Pagos

La app construye app links reales de Adyen para entornos test/live y realiza la llamada de boarding token de Management API desde la app demo. Las solicitudes Terminal API se envuelven con `adyen/NexoCrypto.kt` usando el formato de protección de comunicaciones locales de Adyen: `NexoBlob` cifrado, `SecurityTrailer` y codificación Base64URL para el parámetro `request` del App Link.

Este sigue siendo un modelo de seguridad de demo porque las credenciales viven en el dispositivo. Para producción, mueve el almacenamiento de credenciales y el trabajo de tokens/sesiones a un backend o componente seguro endurecido.

## Más Documentación

- `docs/ADYEN_SETUP.md`: boarding, reboarding, configuración test/live y troubleshooting.
- `docs/QR_CREDENTIALS.md`: esquema JSON de QR y ejemplos.
