import { Component, Renderer2,ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {BaseTabpanel,Tab} from './basetabpanel';

import { WindowRefService } from '@servoy/public';

import { LoggerFactory } from '@servoy/public';

@Component( {
    selector: 'servoydefault-tablesspanel',
    templateUrl: './tablesspanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTablesspanel extends BaseTabpanel {
    constructor(windowRefService: WindowRefService, cdRef: ChangeDetectorRef, logFactory: LoggerFactory, renderer: Renderer2) {
       super(windowRefService, logFactory, renderer,cdRef);
    }
}
