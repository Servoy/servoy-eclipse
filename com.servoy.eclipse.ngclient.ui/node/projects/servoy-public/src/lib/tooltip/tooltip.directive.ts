import { Directive, HostListener, Input, OnDestroy } from '@angular/core';
import { TooltipService } from './tooltip.service';
import { HTMLTooltipDirective } from './tooltip-html.directive';
import { ServoyPublicService } from '../services/servoy_public.service';

@Directive({
    selector: '[svyTooltip]',
    standalone: false
})
export class TooltipDirective extends HTMLTooltipDirective {

    @Input('svyTooltip') tooltipText: string;

        constructor(tooltipService: TooltipService, private servoyService: ServoyPublicService) {
        super(tooltipService);
    }

    protected getInitialDelay(): number {
        let initialDelay = super.getInitialDelay();
        if (initialDelay === null || isNaN(initialDelay)) initialDelay = this.servoyService.getUIProperty("tooltipInitialDelay");
        return initialDelay;
    }

    protected getDismissDelay(): number {
        let dismissDelay = super.getDismissDelay();
        if (dismissDelay === null || isNaN(dismissDelay)) dismissDelay = this.servoyService.getUIProperty("tooltipDismissDelay");
        return dismissDelay;
    }

}
