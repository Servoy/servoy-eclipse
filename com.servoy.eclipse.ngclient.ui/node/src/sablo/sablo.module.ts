import { NgModule } from '@angular/core';
import { AngularWebStorageModule } from 'angular-web-storage';

import { WindowRefService } from './util/windowref.service'

import { TrustAsHtmlPipe } from './pipes/pipes'

import { WebsocketService } from './websocket.service';
import { ConverterService } from './converter.service'
import { ServicesService } from './services.service'
import { SabloService } from './sablo.service'
import { ServiceChangeHandler } from './util/servicechangehandler'
import { LoggerFactory } from './logger.service'

import { SpecTypesService } from './spectypes.service'
import { SabloDeferHelper} from './defer.service';

@NgModule( {
    declarations: [TrustAsHtmlPipe
    ],
    imports: [
        AngularWebStorageModule
    ],
    providers: [ConverterService,
        SpecTypesService,
        SabloService,
        ServicesService,
        WebsocketService,
        WindowRefService,
        LoggerFactory,
        SabloDeferHelper,
        ServiceChangeHandler],
    exports: [TrustAsHtmlPipe]
} )

export class SabloModule { }