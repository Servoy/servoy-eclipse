import { NgModule } from '@angular/core';
import { WebStorageModule } from './webstorage/webstorage.module';
import { WebsocketService } from './websocket.service';
import { ConverterService } from './converter.service';
import { ServicesService } from './services.service';
import { SabloService } from './sablo.service';
import { ServiceChangeHandler } from './util/servicechangehandler';
import { LoggerFactory } from '@servoy/public';
import { SabloDeferHelper} from './defer.service';
import { LoadingIndicatorService } from './util/loading-indicator/loading-indicator.service';
import { TestabilityService } from './testability.service';

@NgModule( {
    declarations: [],
    imports: [
        WebStorageModule,
    ],
    providers: [ConverterService,
        SabloService,
        ServicesService,
        WebsocketService,
        TestabilityService,
        LoadingIndicatorService,
        LoggerFactory,
        SabloDeferHelper,
        ServiceChangeHandler],
    exports: []
} )

export class SabloModule { }
