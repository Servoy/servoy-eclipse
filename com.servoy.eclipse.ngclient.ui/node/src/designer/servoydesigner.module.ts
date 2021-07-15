import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServoyDesignerRoutingModule } from './servoydesigner-routing.module';
import { ServoyDesignerComponent } from './servoydesigner.component';
import { AllServicesModules } from '../ngclient/allservices.service';
import { AllComponentsModule } from '../ngclient/allcomponents.module';
import { ServoyPublicModule, ServoyPublicService } from '@servoy/public';
import { SabloModule } from '../sablo/sablo.module';
import {DesignFormComponent, AddAttributeDirective} from './designform_component.component';
import { ServoyPublicServiceImpl } from '../ngclient/services/servoy_public_impl.service';
import { ServerDataService } from '../ngclient/services/serverdata.service';

@NgModule({
  imports: [
    CommonModule,
    ServoyDesignerRoutingModule,
    AllComponentsModule,
    AllServicesModules,
    ServoyPublicModule,
    SabloModule
  ],
  declarations: [ServoyDesignerComponent, DesignFormComponent, AddAttributeDirective],
  providers: [ServerDataService, ServoyPublicServiceImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceImpl }],
  schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyDesignerModule { }
