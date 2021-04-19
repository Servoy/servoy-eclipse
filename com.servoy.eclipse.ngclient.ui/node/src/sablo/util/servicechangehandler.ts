import { Injectable, } from '@angular/core';

import { SabloService } from '../../sablo/sablo.service';

@Injectable()
export class ServiceChangeHandler {
    constructor(private sabloService: SabloService) {
    }

    public changed(serviceName: string,propertyName: string, propertyValue: any) {
        this.sabloService.sendServiceChanges(serviceName, propertyName, propertyValue);
    }
}
