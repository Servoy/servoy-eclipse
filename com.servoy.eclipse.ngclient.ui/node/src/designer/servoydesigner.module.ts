import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServoyDesignerRoutingModule } from './servoydesigner-routing.module';
import { ServoyDesignerComponent } from './servoydesigner.component';
import { AllServicesModules } from '../ngclient/allservices.service';
import { AllComponentsModule } from '../ngclient/allcomponents.module';
import { ServoyPublicModule, ServoyPublicService } from '@servoy/public';
import { DesignFormComponent } from './designform_component.component';
import { ServoyPublicServiceDesignerImpl } from './servoy_public_designer_impl.service';
import { ServerDataService } from '../ngclient/services/serverdata.service';
import { EditorContentService} from './editorcontent.service';
import { BSWindowManager } from '../ngclient/services/bootstrap-window/bswindow_manager.service';
import { ServoyCoreComponentsModule } from '../servoycore/servoycore.module';

@NgModule({
  imports: [
    CommonModule,
    ServoyDesignerRoutingModule,
    AllComponentsModule,
    AllServicesModules,
    ServoyPublicModule,
    ServoyCoreComponentsModule
  ],
  declarations: [ServoyDesignerComponent, DesignFormComponent ],
  providers: [EditorContentService, BSWindowManager, ServerDataService, ServoyPublicServiceDesignerImpl,
            { provide: ServoyPublicService, useExisting: ServoyPublicServiceDesignerImpl }],
  schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyDesignerModule { }
