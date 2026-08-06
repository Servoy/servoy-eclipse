import { provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';


import { EditorSessionService } from './designer/services/editorsession.service';
import { URLParserService } from './designer/services/urlparser.service';
import { WindowRefService, ServoyPublicModule } from '@servoy/public';
import { DesignSizeService } from './designer/services/designsize.service';
import { DesignerUtilsService } from './designer/services/designerutils.service';
import { EditorContentService } from './designer/services/editorcontent.service';
import { DynamicGuidesService } from './designer/services/dynamicguides.service';
import { provideHttpClient, withXhr, withInterceptorsFromDi } from '@angular/common/http';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { DesignerComponent } from './designer/designer.component';

bootstrapApplication(DesignerComponent, {
    providers: [
        importProvidersFrom(BrowserModule, ServoyPublicModule, FormsModule, CommonModule, NgbModule, DragDropModule),
        EditorSessionService, URLParserService, WindowRefService, DesignSizeService, DesignerUtilsService,
        EditorContentService, DynamicGuidesService, provideHttpClient(withXhr(), withInterceptorsFromDi())
    ]
})
  .catch(err => console.error(err));
