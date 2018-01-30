import { NgModule } from '@angular/core';

import {WindowRefService} from './util/windowref.service'

import {WebsocketService} from './websocket.service';
import {ConverterService} from './converter.service'
import {ServicesService} from './services.service'

@NgModule({
    declarations: [
    ],
    imports: [
    ],
    providers: [ConverterService,ServicesService,WebsocketService,WindowRefService],
  })
  export class SabloModule { }