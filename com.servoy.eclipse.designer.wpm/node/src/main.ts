import { enableProdMode, provideZoneChangeDetection } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { WpmModule } from './wpm/wpm.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(WpmModule, { applicationProviders: [provideZoneChangeDetection()], })
  .catch(err => console.error(err));

