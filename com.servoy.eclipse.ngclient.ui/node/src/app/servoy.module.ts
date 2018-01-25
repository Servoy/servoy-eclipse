import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AllComponentsModule } from './allcomponents.module';


import { MainComponent } from './main.component';
import {WebsocketService} from './websocket.service';
import {FormService} from './form.service';
import {WindowRefService} from './util/windowref.service'
import {AllServicesModules} from './allservices.service';

import { FormComponent,AddAttributeDirective } from './svy-form/svy-form.component';


@NgModule({
  declarations: [
    MainComponent,
    FormComponent,
    AddAttributeDirective,
  ],
  imports: [
    BrowserModule,
    AllComponentsModule,
    AllServicesModules
  ],
  
  providers: [WebsocketService,FormService,WindowRefService],
  bootstrap: [MainComponent]
})
export class ServoyModule { }
