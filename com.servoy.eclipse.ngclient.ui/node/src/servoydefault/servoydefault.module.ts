import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';


import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultButton } from './button/button';


@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton
  ],
  imports:[
    FormsModule],
  exports: [
     ServoyDefaultTextField,
     ServoyDefaultButton
   ],
  providers: []
})
export class ServoyDefaultComponentsModule { }
