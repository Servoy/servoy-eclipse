import { NgModule } from '@angular/core';

import { Injectable } from '@angular/core';

import {WebsocketService} from '../sablo/websocket.service';

import {NGUtilsService} from '../servoy_ng_only_services/ngutils/ngutils.service';

/**
 * this is a the all services that should also be a generated ts file.
 * This should have in providers of the @NgModule below have a list of a all the services that are in the servoy workspace/active project
 * then in the constructor it should be a list by name->class 
 */
@Injectable()
export class AllServiceService {
    constructor(private websocket:WebsocketService, private ngclientutils:NGUtilsService) {
        this.websocket.messages.filter(message=>message.service != null).subscribe(message=>{
           const service = this[message.service as string];
           if (service) {
               if (message.property) {
                   service[message.property as string] = message.value;
               }
               else if (message.call) {
                   var proto = Object.getPrototypeOf(service)
                   proto[message.call as string].apply(service,message.args);
               }
           }
        })
    }
}

@NgModule({
    providers: [AllServiceService,NGUtilsService],
 })
export class AllServicesModules { }