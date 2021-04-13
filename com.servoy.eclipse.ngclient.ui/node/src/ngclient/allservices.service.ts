import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import { ServicesService, ServiceProvider } from '../sablo/services.service';

import { ApplicationService } from './services/application.service';
import { WindowService } from './services/window.service';
import { SessionService } from './services/session.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { WindowService as WindowPlugin} from '../window_service/window.service';

// generated services start
import { WindowServiceModule} from '../window_service/windowservice.module';
import { DialogModule } from '../dialogservice/dialog.module';
import { KeyListener } from '../keylistener/keylistener.service';
import { NGUtilsService } from '../servoy_ng_only_services/ngutils/ngutils.service';
import { DatagridService } from '../nggrids/datagrid/datagrid.service';
import { PowergridService } from '../nggrids/powergrid/powergrid.service';
import { DialogService } from '../dialogservice/dialogs.service';
import { ClientFunctionService } from './services/clientfunction.service';
import { SabloService } from '../sablo/sablo.service';
import { NGDesktopFileService } from '@servoy/ngdesktopfile';
import { NGDesktopUtilsService } from '@servoy/ngdesktoputils';
import { NGDesktopUIService } from '@servoy/ngdesktopui';
// generated services end


/**
 * this is a the all services that should also be a generated ts file.
 * This should have in providers of the @NgModule below have a list of a all the services that are in the servoy workspace/active project
 * then in the constructor it should be a list by name->class
 * so this should only have services that are called from the server side (internals like $applicationService or externals like keylistener)
 */
@Injectable()
export class AllServiceService implements ServiceProvider {
    constructor( private services: ServicesService,
        private $applicationService: ApplicationService,
        private $windowService: WindowService,
        private $sabloLoadingIndicator: LoadingIndicatorService,
        private $sessionService: SessionService,
        private $sabloService: SabloService,
        private clientFunctionService: ClientFunctionService,
        // generated services start
        private ngclientutils: NGUtilsService,
        private keyListener: KeyListener,
        private window: WindowPlugin,
        private ngDataGrid: DatagridService,
        private ngPowerGrid: PowergridService,
        private dialogs: DialogService,
        private ngdesktopfile: NGDesktopFileService,
        private ngdesktoputils: NGDesktopUtilsService,
        private ngdesktopui: NGDesktopUIService
        // generated services end
    ) {
        services.setServiceProvider( this );
    }
    getService( name: string ) {
        return this[name];
    }

    init() {
        // just here is it can be called on.
    }

}

@NgModule( {
    providers: [AllServiceService, ApplicationService, WindowService, SessionService, ClientFunctionService,
                // generated services start (only for dynamic services (servoy plugins))
                NGUtilsService,
                KeyListener,
                DatagridService,
                PowergridService,
                NGDesktopFileService,
                NGDesktopUtilsService,
                NGDesktopUIService
                // generated services end
                ],
    imports: [
                // generated services modules start (only for dynamic services (servoy plugins))
                WindowServiceModule,
                DialogModule
                // generated services modules end.
                ]
} )
export class AllServicesModules { }
