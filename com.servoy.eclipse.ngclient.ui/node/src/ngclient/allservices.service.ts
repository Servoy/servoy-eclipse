import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import { ServicesService, ServiceProvider } from '../sablo/services.service';

import { ApplicationService } from './services/application.service';
import { ClientDesignService } from './services/clientdesign.service';
import { ClientUtilsService } from './services/clientutils.service';
import { WindowService } from './services/window.service';
import { SessionService } from './services/session.service';
import {PopupFormService} from './services/popupform.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { ClientFunctionService } from '../sablo/clientfunction.service';
import { SabloService } from '../sablo/sablo.service';
import { TypesRegistry } from '../sablo/types_registry';
// generated imports start
// generated imports end


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
        private clientdesign: ClientDesignService,
        private clientutils: ClientUtilsService,
        private $windowService: WindowService,
        private $sabloLoadingIndicator: LoadingIndicatorService,
        private $sessionService: SessionService,
        private $sabloService: SabloService,
        private $typesRegistry: TypesRegistry,
        // generated services start
        // generated services end
        private clientFunctionService: ClientFunctionService,
    ) {
        services.setServiceProvider( this );
    }
    getService( name: string ) {
        return this[name];
    }

    init() {
        Object.keys(this).forEach(key => {
            if (typeof this[key].init === 'function') {
                this[key].init();
            }
        });
    }

}

@NgModule( {
    providers: [AllServiceService, ApplicationService, ClientDesignService, ClientUtilsService, WindowService, SessionService, PopupFormService,
                // generated providers start
                // generated providers end
                ],
    imports: [
                // generated modules start
                // generated modules end
                ]
} )
export class AllServicesModules { }
