// The application runs zoneless (provideZonelessChangeDetection in main.ts), so zone.js is
// NOT included by default. The TiNG build generator (WebPackagesListener) adds "zone.js" to
// the angular.json polyfills array on demand when the developer sets -Dti.ng.includezonejs=true,
// which re-enables async patching so NG0100 ExpressionChangedAfterItHasBeenCheckedError
// warnings surface again. Do not import 'zone.js' here.
