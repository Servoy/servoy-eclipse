import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import {ServicesService, ServiceProvider} from '../sablo/services.service';

import {NGUtilsService} from '../servoy_ng_only_services/ngutils/ngutils.service';

/**
 * this is a the all services that should also be a generated ts file.
 * This should have in providers of the @NgModule below have a list of a all the services that are in the servoy workspace/active project
 * then in the constructor it should be a list by name->class 
 */
@Injectable()
export class AllServiceService implements ServiceProvider {
    constructor(private services:ServicesService, private ngclientutils:NGUtilsService) {
        services.setServiceProvider(this);
    }
    getService(name:string) {
        return this[name];
    }
} 

@NgModule({
    providers: [AllServiceService,NGUtilsService],
 })
export class AllServicesModules { }