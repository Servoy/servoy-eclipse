import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ServoyDesignerRoutingModule } from './servoydesigner-routing.module';
import { ServoyDesignerComponent } from './servoydesigner.component';
import { ServoyPublicService } from '@servoy/public';
import { DesignFormComponent } from './designform_component.component';
import { ServoyPublicServiceDesignerImpl } from './servoy_public_designer_impl.service';
import { ServerDataService } from '../ngclient/services/serverdata.service';
import { EditorContentService} from './editorcontent.service';
import { BSWindowManager } from '../ngclient/services/bootstrap-window/bswindow_manager.service';
import { LFCModule } from '../ngclient/lfc.module';

@NgModule({
  imports: [
    ServoyDesignerRoutingModule,
    LFCModule
  ],
  declarations: [ServoyDesignerComponent, DesignFormComponent],
  providers: [EditorContentService, BSWindowManager, ServerDataService, ServoyPublicServiceDesignerImpl,
            { provide: ServoyPublicService, useExisting: ServoyPublicServiceDesignerImpl }],
  schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyDesignerModule { }
