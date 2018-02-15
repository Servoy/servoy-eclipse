import { NgModule } from '@angular/core';
import { AngularWebStorageModule } from 'angular-web-storage';

import {WindowRefService} from './util/windowref.service'

import {TrustAsHtmlPipe} from  './pipes/pipes'

import {WebsocketService} from './websocket.service';
import {ConverterService} from './converter.service'
import {ServicesService} from './services.service'
import {SabloService} from './sablo.service'

@NgModule({
    declarations: [TrustAsHtmlPipe
    ],
    imports: [
              AngularWebStorageModule
    ],
    providers: [ConverterService,
                        SabloService,
                        ServicesService,
                        WebsocketService,
                        WindowRefService],
    exports:[TrustAsHtmlPipe]
  })
  export class SabloModule { }