import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { AllComponentsModule } from './allcomponents.module';


import { MainComponent } from './main.component';
import {FormService} from './form.service';
import {AllServicesModules} from './allservices.service';

import { FormComponent,AddAttributeDirective } from './svy-form/svy-form.component';

import {SabloModule} from '../sablo/sablo.module'


@NgModule({
  declarations: [
    MainComponent,
    FormComponent,
    AddAttributeDirective,
  ],
  imports: [
    BrowserModule,
    SabloModule,
    AllComponentsModule,
    AllServicesModules
  ],
  
  providers: [FormService],
  bootstrap: [MainComponent]
})
export class ServoyModule { }
