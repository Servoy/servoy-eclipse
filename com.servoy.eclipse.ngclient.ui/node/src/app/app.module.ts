import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { ComponentModule } from '../components/component.module';


import { AppComponent } from './app.component';
import {WebsocketService} from './websocket.service';
import {FormService} from './form.service';
import {AllServicesModules} from './allservices.service';

import { FormComponent,AddAttributeDirective } from './svy-form/svy-form.component';


@NgModule({
  declarations: [
    AppComponent,
    FormComponent,
    AddAttributeDirective,
  ],
  imports: [
    BrowserModule,
    ComponentModule,
    AllServicesModules
  ],
  
  providers: [WebsocketService,FormService],
  bootstrap: [AppComponent]
})
export class AppModule { }
