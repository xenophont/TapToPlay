# QR Credential Profiles

TapToPlay imports Adyen payment profiles by scanning a QR code that contains raw JSON. Profiles are stored in encrypted local storage and must be selected deliberately in the `Payments App` tab.

Treat every credential QR as a secret. It contains API credentials and Terminal API encryption material.

## Credential JSON Format

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

## Live Example Shape

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Boutique Madrid LIVE",
  "environment": "live",
  "merchantId": "YourLiveMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "live_AQE...",
  "clientKey": "live_...",
  "terminalKeyIdentifier": "LiveCryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "live-shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

Use placeholder values in documentation and examples. Never commit a real live payload.

## Profile Label Resolution

For merchant-scoped profiles without `storeId`, TapToPlay uses the existing `merchantId` directly as the primary profile label. No extra Management API lookup is needed.

For store-scoped profiles, `displayName` is kept as the QR fallback label. When `storeId` is present, TapToPlay uses the scanned `apiKey` with the Adyen Management API v3 `GET /stores` endpoint, filtered by `merchantId`, to find the matching store. The store `reference` is stored locally as `storeName` and becomes the primary profile label in profile selection, checkout, and diagnostics.

The API credential must include the Adyen Management API stores read role. If the lookup fails or the store cannot be found, TapToPlay still stores the profile with the original `displayName` so boarding is not blocked.

## Validation Rules

- `schema` must be `taptoplay.adyen.profile.v1`.
- Payloads larger than 8192 characters are rejected.
- `environment` must be `test` or `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, terminal key fields, `currency`, and `countryCode` are required.
- `displayName` must be 80 characters or less.
- `storeName` is optional and normally app-populated after scan. If present, it requires `storeId` and must be 300 characters or less.
- `merchantId`, `storeId`, and `terminalKeyIdentifier` must be 128 characters or less.
- `apiKey`, `clientKey`, and `terminalPassphrase` must be 512 characters or less.
- `storeId` is optional. When present, TapToPlay requests store-scoped boarding and Payments App instance listing.
- `currency` must be an uppercase ISO 4217 code such as `EUR`.
- `countryCode` must be an uppercase ISO 3166-1 alpha-2 code such as `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion`, and `terminalPassphrase` must match the shared key configured in the Adyen Customer Area for Terminal API encryption.

Unknown fields are rejected for credential QR payloads. This keeps the credential schema tight and prevents accidental extra data from becoming part of a stored profile.

## Creating and Using a Credential QR

1. Fill the JSON with your Adyen test or live values.
2. Generate a QR code from the raw JSON text using a trusted offline QR tool, or an internal tool you control.
3. Do not upload real credential payloads to public QR services.
4. Open `Payments App` in TapToPlay.
5. Tap `Scan QR`.
6. Select the scanned profile deliberately.
7. Run `Check` and `Board` before charging.

For production-like demos, generate separate QR codes for test and live profiles so switching remains explicit.

## Credential Lifecycle

TapToPlay stores scanned profiles in encrypted preferences and masks secrets in the UI. The `Payments App` tab includes:

- `Remove`: deletes the local profile and clears saved local boarding state for that profile.
- `Refresh`: lists Adyen Payments App instances using the selected profile API key.
- `Revoke instance`: revokes a selected Adyen Payments App installation after confirmation.

Removing a profile does not revoke an Adyen Payments App installation. Revoke the instance separately when you need to invalidate the boarded app/device.

## SaleToAcquirerData QR Codes

Credential QR codes use `taptoplay.adyen.profile.v1`. SaleToAcquirerData QR codes are separate and must contain the plain Adyen SaleToAcquirerData JSON object so they can be scanned from `Checkout` without changing the active payment profile.

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

Rules:

- The root object is the SaleToAcquirerData object itself.
- TapToPlay Base64-encodes this object exactly as scanned and writes it into `PaymentRequest.SaleData.SaleToAcquirerData`.
- Payloads larger than 12288 characters are rejected.
- The parsed object can contain at most 80 leaf fields.
- TapToPlay wrapper fields such as `schema`, `displayName`, `saleToAcquirerData`, and `properties` are rejected at the root.
- Values may be strings, numbers, booleans, arrays, or nested objects.

## Storage and Backup Notes

The app disables Android backup and explicitly excludes encrypted preference files used for profiles, boarding state, transaction history, and SaleToAcquirerData favorites. This reduces accidental credential movement between devices, but it does not turn an in-app credential model into a production architecture.

---

# Perfiles de credenciales QR

TapToPlay importa perfiles de pago de Adyen escaneando un código QR que contiene JSON sin procesar. Los perfiles se guardan en almacenamiento local cifrado y deben seleccionarse de forma deliberada en la pestaña `Payments App`.

Trata cada QR de credenciales como un secreto. Contiene credenciales de API y material de cifrado de Terminal API.

## Formato JSON de credenciales

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

## Ejemplo de estructura live

```json
{
  "schema": "taptoplay.adyen.profile.v1",
  "displayName": "Boutique Madrid LIVE",
  "environment": "live",
  "merchantId": "YourLiveMerchantAccount",
  "storeId": "ST322LJ223223K5F",
  "apiKey": "live_AQE...",
  "clientKey": "live_...",
  "terminalKeyIdentifier": "LiveCryptoKeyIdentifier",
  "terminalKeyVersion": 1,
  "terminalPassphrase": "live-shared-key-passphrase",
  "currency": "EUR",
  "countryCode": "ES"
}
```

Usa valores de ejemplo en la documentación y en los ejemplos. Nunca comitees un payload live real.

## Resolucion de etiqueta de perfil

En perfiles con alcance de merchant, sin `storeId`, TapToPlay usa directamente el `merchantId` existente como etiqueta principal del perfil. No hace falta otro lookup de Management API.

En perfiles con alcance de tienda, `displayName` queda como etiqueta fallback del QR. Cuando existe `storeId`, TapToPlay usa la `apiKey` escaneada con el endpoint `GET /stores` de Adyen Management API v3, filtrado por `merchantId`, para encontrar la tienda correspondiente. El `reference` de la store se guarda localmente como `storeName` y pasa a ser la etiqueta principal del perfil en seleccion, checkout y diagnosticos.

La credencial API debe tener el rol de lectura de stores de Management API. Si el lookup falla o no se encuentra la tienda, TapToPlay guarda igualmente el perfil con el `displayName` original para no bloquear el boarding.

## Reglas de validación

- `schema` debe ser `taptoplay.adyen.profile.v1`.
- Se rechazan los payloads de más de 8192 caracteres.
- `environment` debe ser `test` o `live`.
- `displayName`, `merchantId`, `apiKey`, `clientKey`, los campos de clave de terminal, `currency` y `countryCode` son obligatorios.
- `displayName` debe tener 80 caracteres o menos.
- `storeName` es opcional y normalmente lo rellena la app despues del escaneo. Si aparece, requiere `storeId` y debe tener 300 caracteres o menos.
- `merchantId`, `storeId` y `terminalKeyIdentifier` deben tener 128 caracteres o menos.
- `apiKey`, `clientKey` y `terminalPassphrase` deben tener 512 caracteres o menos.
- `storeId` es opcional. Cuando está presente, TapToPlay solicita boarding y listado de instancias de Payments App con alcance de tienda.
- `currency` debe ser un código ISO 4217 en mayúsculas, como `EUR`.
- `countryCode` debe ser un código ISO 3166-1 alfa-2 en mayúsculas, como `ES`.
- `terminalKeyIdentifier`, `terminalKeyVersion` y `terminalPassphrase` deben coincidir con la clave compartida configurada en el Customer Area de Adyen para el cifrado de Terminal API.

Los campos desconocidos se rechazan en los payloads QR de credenciales. Esto mantiene el esquema de credenciales ajustado y evita que datos accidentales formen parte de un perfil guardado.

## Crear y usar un QR de credenciales

1. Rellena el JSON con tus valores de Adyen de test o live.
2. Genera un código QR a partir del texto JSON sin procesar usando una herramienta QR offline de confianza o una herramienta interna que controles.
3. No subas payloads con credenciales reales a servicios QR públicos.
4. Abre `Payments App` en TapToPlay.
5. Toca `Scan QR`.
6. Selecciona deliberadamente el perfil escaneado.
7. Ejecuta `Check` y `Board` antes de cobrar.

Para demos similares a producción, genera códigos QR separados para perfiles test y live, de forma que el cambio de entorno siga siendo explícito.

## Ciclo de vida de credenciales

TapToPlay guarda los perfiles escaneados en preferencias cifradas y enmascara los secretos en la interfaz. La pestaña `Payments App` incluye:

- `Remove`: elimina el perfil local y borra el estado local de boarding guardado para ese perfil.
- `Refresh`: lista las instancias de Adyen Payments App usando la API key del perfil seleccionado.
- `Revoke instance`: revoca una instalación seleccionada de Adyen Payments App después de una confirmación.

Eliminar un perfil no revoca una instalación de Adyen Payments App. Revoca la instancia por separado cuando necesites invalidar la app o el dispositivo ya boarded.

## Códigos QR de SaleToAcquirerData

Los QR de credenciales usan `taptoplay.adyen.profile.v1`. Los QR de SaleToAcquirerData son independientes y deben contener el objeto JSON plano de Adyen SaleToAcquirerData, para poder escanearlos desde `Checkout` sin cambiar el perfil de pago activo.

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

Reglas:

- El objeto raíz es el propio objeto SaleToAcquirerData.
- TapToPlay codifica en Base64 este objeto exactamente como se escaneó y lo escribe en `PaymentRequest.SaleData.SaleToAcquirerData`.
- Se rechazan los payloads de más de 12288 caracteres.
- El objeto parseado puede contener como máximo 80 campos hoja.
- Los campos wrapper de TapToPlay, como `schema`, `displayName`, `saleToAcquirerData` y `properties`, se rechazan en la raíz.
- Los valores pueden ser cadenas, números, booleanos, arrays u objetos anidados.

## Notas de almacenamiento y backup

La app desactiva el backup de Android y excluye explícitamente los archivos de preferencias cifradas usados para perfiles, estado de boarding, historial de transacciones y favoritos de SaleToAcquirerData. Esto reduce el movimiento accidental de credenciales entre dispositivos, pero no convierte un modelo de credenciales dentro de la app en una arquitectura de producción.
