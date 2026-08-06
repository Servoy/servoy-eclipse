import { provideZoneChangeDetection } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { DesignerModule } from './designer/designer.module';

platformBrowserDynamic().bootstrapModule(DesignerModule, { applicationProviders: [provideZoneChangeDetection()], })
  .catch(err => console.error(err));
