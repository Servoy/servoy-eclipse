import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ServoyDesignerComponent } from './servoydesigner.component';
import { ServoyPublicService } from '@servoy/public';
import { DesignFormComponent } from './designform_component.component';
import { ServoyPublicServiceDesignerImpl } from './servoy_public_designer_impl.service';
import { ServerDataService } from '../ngclient/services/serverdata.service';
import { EditorContentService} from './editorcontent.service';
import { BSWindowManager } from '../ngclient/services/bootstrap-window/bswindow_manager.service';
import { ServoyCoreComponentsModule } from '../servoycore/servoycore.module';
import { DESIGNER_ROUTES } from './servoydesigner.routes';

@NgModule({
  imports: [
    RouterModule.forChild(DESIGNER_ROUTES),
    ServoyCoreComponentsModule,
    ServoyDesignerComponent,
    DesignFormComponent
  ],
  providers: [EditorContentService, BSWindowManager, ServerDataService, ServoyPublicServiceDesignerImpl,
            { provide: ServoyPublicService, useExisting: ServoyPublicServiceDesignerImpl }]
})
export class ServoyDesignerModule { }
