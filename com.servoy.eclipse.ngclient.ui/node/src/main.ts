import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideZonelessChangeDetection, provideCheckNoChangesConfig, enableProdMode, isDevMode } from '@angular/core';

import { AppComponent } from './app/app.component';
import { APP_ROUTES } from './app/app.routes';
import { environment } from './environments/environment';
import { provideAgGrid } from './servoycore/ag-grid-initializer';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(APP_ROUTES),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    provideZonelessChangeDetection(),
    provideAgGrid(),
    ...(isDevMode() ? [provideCheckNoChangesConfig({ exhaustive: true, interval: 500 })] : []),
  ],
}).catch((err) => console.log(err));
