import { provideZonelessChangeDetection } from '@angular/core';

import { EditorSessionService } from './designer/services/editorsession.service';
import { URLParserService } from './designer/services/urlparser.service';
import { WindowRefService } from '@servoy/public';
import { DesignSizeService } from './designer/services/designsize.service';
import { DesignerUtilsService } from './designer/services/designerutils.service';
import { EditorContentService } from './designer/services/editorcontent.service';
import { DynamicGuidesService } from './designer/services/dynamicguides.service';
import { provideHttpClient, withXhr, withInterceptorsFromDi } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { DesignerComponent } from './designer/designer.component';

bootstrapApplication(DesignerComponent, {
    providers: [
        EditorSessionService, URLParserService, WindowRefService, DesignSizeService, DesignerUtilsService,
        EditorContentService, DynamicGuidesService, provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideZonelessChangeDetection()
    ]
})
  .catch(err => console.error(err));
