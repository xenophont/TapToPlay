Drop SaleToAcquirerData preset JSON files in this folder.

The app loads every `.json` file here into the Defaults picker at startup. A file can be either:

- a plain Adyen SaleToAcquirerData JSON object, or
- a TapToPlay preset wrapper with `schema`, `displayName`, `mergeWithDefaults`, and `data`.

Never put live credentials here. These files are packaged into the app.
