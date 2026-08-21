import { Component, Renderer2,ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {BaseTabpanel,Tab} from './basetabpanel';

import { WindowRefService, ServoyPublicService } from '@servoy/public';

import { LoggerFactory } from '@servoy/public';

@Component( {
    selector: 'servoydefault-tablesspanel',
    templateUrl: './tablesspanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTablesspanel extends BaseTabpanel {

    containerStyle: { [property: string]: any } = { overflow: 'auto' };

    constructor(windowRefService: WindowRefService, cdRef: ChangeDetectorRef, logFactory: LoggerFactory, renderer: Renderer2, servoyPublicService: ServoyPublicService) {
       super(windowRefService, logFactory, renderer, cdRef, servoyPublicService);
    }

    getContainerStyle(): { [property: string]: any } {
        this.containerStyle['border'] = this.borderStyle;
        this.applyOverflowFromForm(this.containerStyle);
        return this.containerStyle;
    }
}
