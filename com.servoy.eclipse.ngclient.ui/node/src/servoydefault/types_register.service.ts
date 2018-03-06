import { Injectable } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service'
import {Tab} from './tabpanel/basetabpanel'

// this should be in the spec that this is a service that should be included 
@Injectable()
export class TypesRegisterService {

    constructor( specTypesService: SpecTypesService ) {
        // TODO maybe the property names array should come from the server.. so we don't have to include it twice (in spec file and here)
        specTypesService.registerType("servoydefault-tabpanel.tab", Tab, ["name","containsFormId","text", "relationName", "active","foreground","disabled","imageMediaID" ,"mnemonic", "isActive"]);  
    }
}