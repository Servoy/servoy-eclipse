import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';


import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultButton } from './button/button';

import {SabloModule} from '../sablo/sablo.module'

import {ServoyApiModule} from '../servoyapi/servoy_api.module'

@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton
  ],
  imports:[
    FormsModule,
    SabloModule,
    ServoyApiModule],
  exports: [
     ServoyDefaultTextField,
     ServoyDefaultButton
   ],
  providers: []
})
export class ServoyDefaultComponentsModule { }
