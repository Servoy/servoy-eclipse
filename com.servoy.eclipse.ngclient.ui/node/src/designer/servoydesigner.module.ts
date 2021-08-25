import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServoyDesignerRoutingModule } from './servoydesigner-routing.module';
import { ServoyDesignerComponent } from './servoydesigner.component';
import { AllServicesModules } from '../ngclient/allservices.service';
import { AllComponentsModule } from '../ngclient/allcomponents.module';
import { ServoyPublicModule, ServoyPublicService } from '@servoy/public';
import {DesignFormComponent, AddAttributeDirective} from './designform_component.component';
import { ServoyPublicServiceDesignerImpl } from './servoy_public_designer_impl.service';
import { ServerDataService } from '../ngclient/services/serverdata.service';
import {EditorContentService} from './editorcontent.service';
import { BSWindow } from '../ngclient/services/bootstrap-window/bswindow.service';
import { BSWindowManager } from '../ngclient/services/bootstrap-window/bswindow_manager.service';

@NgModule({
  imports: [
    CommonModule,
    ServoyDesignerRoutingModule,
    AllComponentsModule,
    AllServicesModules,
    ServoyPublicModule
  ],
  declarations: [ServoyDesignerComponent, DesignFormComponent, AddAttributeDirective],
  providers: [EditorContentService, BSWindow, BSWindowManager, ServerDataService, ServoyPublicServiceDesignerImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceDesignerImpl }],
  schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyDesignerModule { }
