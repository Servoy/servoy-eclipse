import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';
import { CommonModule } from '@angular/common';


import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultButton } from './button/button';
import { ServoyDefaultLabel} from './label/label';

import {SabloModule} from '../sablo/sablo.module'

import {ServoyApiModule} from '../servoyapi/servoy_api.module'

@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton,
    ServoyDefaultLabel
  ],
  imports:[
    FormsModule,
    CommonModule,
    SabloModule,
    ServoyApiModule],
  exports: [
     ServoyDefaultTextField,
     ServoyDefaultButton,
     ServoyDefaultLabel
   ],
  providers: []
})
export class ServoyDefaultComponentsModule { }
