import { Injectable } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service';
import {MenuItem} from './navbar/navbar';

// this should be in the spec that this is a service that should be included
@Injectable()
export class TypesBootstrapExtraRegisterService {

    constructor( specTypesService: SpecTypesService ) {
         specTypesService.registerType('bootstrapextracomponents-navbar.menuItem', MenuItem, ['isActive','dataProvider']);
    }
}
