import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ServoyPublicService } from '@servoy/public';
import { FormTemplateRoutingModule } from './formtemplate-routing.module';
import { ServoyFormTemplateComponent } from './formtemplate.component';
import { FormTemplateComponent } from './formtemplate_component.component';
import { ServoyPublicServiceFormTemplateImpl } from './servoy_public_formtemplate_impl.service';
import { LFCModule } from '../ngclient/lfc.module';

@NgModule({
  imports: [
    FormTemplateRoutingModule,
    LFCModule
  ],
  declarations: [ServoyFormTemplateComponent, FormTemplateComponent],
  providers: [ServoyPublicServiceFormTemplateImpl,
            { provide: ServoyPublicService, useExisting: ServoyPublicServiceFormTemplateImpl }],
  schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class FormTemplateModule { }
