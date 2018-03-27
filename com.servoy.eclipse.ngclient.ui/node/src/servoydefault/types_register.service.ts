import { Injectable } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service'
import {Tab} from './tabpanel/basetabpanel'

// this should be in the spec that this is a service that should be included 
@Injectable()
export class TypesRegisterService {

    constructor( specTypesService: SpecTypesService ) {
        // TODO maybe the property names array should come from the server.. so we don't have to include it twice (in spec file and here)
        specTypesService.registerType("servoydefault-tabpanel.tab", Tab, ["name","containsFormId","text", "relationName", "foreground","disabled","imageMediaID" ,"mnemonic"]);  
        // register the same type for the splitpane
        specTypesService.registerType("servoydefault-splitpane.tab", Tab, ["name","containsFormId","text", "relationName", "foreground","disabled","imageMediaID" ,"mnemonic"]);
    }
}