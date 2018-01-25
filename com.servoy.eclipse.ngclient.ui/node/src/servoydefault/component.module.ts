import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';


import { SvyTextfield } from './textfield/textfield';
import { SvyButton } from './button/button';


@NgModule({
  declarations: [
    SvyTextfield,
    SvyButton
  ],
  imports:[
    FormsModule],
  exports: [
     SvyTextfield,
     SvyButton
   ],
  providers: []
})
export class ComponentModule { }
