import { enableProdMode, provideZoneChangeDetection } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { DesignerModule } from './designer/designer.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(DesignerModule, { applicationProviders: [provideZoneChangeDetection()], })
  .catch(err => console.error(err));

