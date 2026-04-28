# TapToPlay

TapToPlay es una demo retail en Kotlin/Compose para una tienda premium de ropa usando la app Android Payments de Adyen para Tap to Pay. Incluye catalogo boutique, carrito, checkout, enlaces de boarding/reboarding de Adyen, lanzamiento de pagos, importacion de perfiles por QR y almacenamiento local cifrado de perfiles.

## Aviso de Seguridad de la Demo

Adyen recomienda crear las solicitudes de sesion y boarding desde un backend. Este proyecto permite perfiles con credenciales dentro de la app para que la demo se pueda ejecutar facilmente desde un unico dispositivo Android. Eso mantiene los secretos fuera de git, pero no hace que los secretos sean seguros dentro de un APK distribuido.

Usa esta arquitectura solo para demos. Una app de produccion deberia mover las API keys, las llamadas de boarding token y el material de cifrado de Terminal API a un backend o a una integracion segura de nivel produccion.

## Configuracion Local

1. Abre el proyecto en Android Studio.
2. Instala la app Adyen Payments Test en un dispositivo Android compatible.
3. Anade credenciales bootstrap opcionales en `local.properties`.
4. Ejecuta la app debug en el dispositivo.
5. Escanea un perfil de credenciales por QR o usa el perfil bootstrap local.
6. Toca `Check` para que la app Adyen Payments devuelva el estado de boarding.
7. Si la app no esta boarded, toca `Board` para intercambiar el boarding request token devuelto y completar la configuracion.
8. Anade productos del catalogo e inicia el checkout.

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

`local.properties` esta ignorado por git. No pongas credenciales reales en archivos versionados.

## Comandos Utiles

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
3. Trata la imagen QR como una contrasena porque contiene API keys y material de cifrado de terminal.
4. Instala y abre TapToPlay, toca `Scan QR`, escanea el perfil y seleccionalo en el panel de payment profile.

No comitees imagenes QR ni archivos JSON con credenciales reales. Para mas detalle y un ejemplo live, revisa `docs/QR_CREDENTIALS.md`.

## Crear un QR de SaleToAcquirerData

TapToPlay tambien puede escanear un QR para reemplazar el objeto `SaleToAcquirerData` de las siguientes solicitudes de pago. Esto es util para probar features de Adyen controladas mediante `SaleToAcquirerData`.

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
  "shopperStatement": "YOUR_PAYMENT_DESCRIPTION",
  "store": "YOUR_STORE_REFERENCE",
  "tenderOption": "ReceiptHandler,AskGratuity"
}
```

Para usarlo:

1. Genera un QR desde el JSON en crudo anterior, reemplazando las propiedades por los valores que quieras probar.
2. Abre TapToPlay y anade productos al carrito.
3. En `Checkout`, toca `Scan data QR`.
4. Confirma que el panel de checkout muestra `Scanned SaleToAcquirerData`.
5. Inicia el pago. La solicitud cifrada de Terminal API incluira tu JSON estructurado como Base64 en `SaleToAcquirerData`.

Usa `Reset` en el panel de checkout para volver a los metadatos por defecto de la demo retail.

Los QR antiguos de TapToPlay con `schema`, `displayName` y `saleToAcquirerData` siguen siendo aceptados, pero los QR nuevos deberian usar el objeto plano anterior para evitar enviar campos wrapper o defaults de demo a Adyen.

## Favoritos de SaleToAcquirerData

Para demos, puedes guardar configuraciones frecuentes de `SaleToAcquirerData` como favoritos:

1. Carga una configuracion con `Scan data QR`, o abre `View` y edita los campos manualmente.
2. Toca `Save` en el panel `SaleToAcquirerData` del checkout o dentro del editor de campos.
3. Usa la lista `Favorites` en checkout y toca `Use` antes de iniciar una transaccion.
4. Toca `Remove` en un favorito cuando ya no necesites ese preset.

Los favoritos se guardan localmente con preferencias Android cifradas. Son presets de demo para acelerar pruebas; no guardes datos sensibles de produccion en APKs de demo.

## Historial de Transacciones, Respuestas y Refunds

Cada intento de pago o refund se guarda en el panel `Transactions`. Toca `Inspect` para revisar:

- La solicitud Terminal API en crudo enviada a la app Adyen Payments.
- La URI de retorno en crudo.
- La respuesta Terminal API decodificada cuando exista.
- Una vista legible de campos con los valores clave de respuesta arriba.
- Datos `AdditionalResponse` decodificados, incluyendo JSON Base64 o valores Base64 dentro de respuestas key-value.

Si una respuesta de pago aprobada incluye un identificador de transaccion de Terminal API, el inspector de transacciones habilita `Refund`. Esto lanza un refund referenciado usando una `ReversalRequest` contra la transaccion original. El resultado del refund puede seguir requiriendo webhooks de Adyen o comprobaciones en Customer Area segun tu configuracion de Adyen.

## Build y Despliegue

Desde Android Studio:

1. Abre esta carpeta de proyecto.
2. Deja que Gradle termine la sincronizacion.
3. Selecciona la configuracion de ejecucion `app`.
4. Conecta un dispositivo Android compatible con NFC.
5. Pulsa Run.

Desde PowerShell:

```powershell
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Para pagos test, instala la app Adyen Payments Test en el mismo dispositivo. Para pagos live, instala la app live de Adyen Payments, escanea un perfil QR live, seleccionalo explicitamente y despues ejecuta `Check`, `Board` y checkout.

## Limite Actual de Pagos

La app construye app links reales de Adyen para entornos test/live y realiza la llamada de boarding token de Management API desde la app demo. Las solicitudes Terminal API se envuelven con `adyen/NexoCrypto.kt` usando el formato de proteccion de comunicaciones locales de Adyen: `NexoBlob` cifrado, `SecurityTrailer` y codificacion Base64URL para el parametro `request` del App Link.

Este sigue siendo un modelo de seguridad de demo porque las credenciales viven en el dispositivo. Para produccion, mueve el almacenamiento de credenciales y el trabajo de tokens/sesiones a un backend o componente seguro endurecido.

## Mas Documentacion

- `docs/ADYEN_SETUP.md`: boarding, reboarding, configuracion test/live y troubleshooting.
- `docs/QR_CREDENTIALS.md`: esquema JSON de QR y ejemplos.
