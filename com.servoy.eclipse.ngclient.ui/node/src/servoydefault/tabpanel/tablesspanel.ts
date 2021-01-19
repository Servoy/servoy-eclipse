import { Component, Renderer2,ChangeDetectorRef, ChangeDetectionStrategy} from '@angular/core';

import {BaseTabpanel,Tab} from './basetabpanel';

import { WindowRefService } from '../../sablo/util/windowref.service';

import { LoggerFactory } from '../../sablo/logger.service';

@Component( {
    selector: 'servoydefault-tablesspanel',
    templateUrl: './tablesspanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush
} )
export class ServoyDefaultTablesspanel extends BaseTabpanel {
    constructor(windowRefService: WindowRefService, cdRef: ChangeDetectorRef, logFactory: LoggerFactory, renderer: Renderer2) {
       super(windowRefService, logFactory, renderer,cdRef);
    }
}
