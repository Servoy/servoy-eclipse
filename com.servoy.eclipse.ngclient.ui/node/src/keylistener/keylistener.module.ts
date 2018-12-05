import { NgModule, CUSTOM_ELEMENTS_SCHEMA, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import { SabloModule } from '../sablo/sablo.module'

import { KeyListener} from './keylistener.service';


@NgModule({
    declarations: [],
    imports: [CommonModule,
              NgbModule,
              SabloModule],
    exports:[],
    providers:[]
})

export class KeylistenerModule { }