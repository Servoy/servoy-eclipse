import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import { ServicesService, ServiceProvider } from '../sablo/services.service';

import { ApplicationService } from './services/application.service'
import { WindowService } from './services/window.service'
import { SessionService } from './services/session.service'

import { KeyListener } from '../keylistener/keylistener.service';
import { ComponentContributor } from './component_contributor.service';
import { SvyUtilsService } from './services/utils.service';

// generated services start
import { NGUtilsService } from '../servoy_ng_only_services/ngutils/ngutils.service';
import { TypesRegisterService } from '../servoydefault/types_register.service';
import { BSWindow } from "./services/bootstrap-window/bswindow.service";
import { BSWindowManager } from "./services/bootstrap-window/bswindow_manager.service";
// generated services end


/**
 * this is a the all services that should also be a generated ts file.
 * This should have in providers of the @NgModule below have a list of a all the services that are in the servoy workspace/active project
 * then in the constructor it should be a list by name->class 
 */
@Injectable()
export class AllServiceService implements ServiceProvider {
    constructor( private services: ServicesService,
        private $applicationService: ApplicationService,
        private $windowService: WindowService,
        private $sessionService: SessionService,
        // generated services start
        private ngclientutils: NGUtilsService,
        private servoydefaultTypesRegisterService: TypesRegisterService,
        private keyListener: KeyListener
        // generated services end
    ) {
        services.setServiceProvider( this );
    }
    getService( name: string ) {
        return this[name];
    }
}

@NgModule( {
    providers: [AllServiceService, ApplicationService, WindowService,SvyUtilsService,SessionService,
                KeyListener,
                // generated services start
                NGUtilsService, 
                TypesRegisterService,
                BSWindow,
                BSWindowManager
                // generated services end
                ],
} )
export class AllServicesModules { }