import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { LoggerFactory, LoggerService, SabloTabseq, TooltipDirective, TrustAsHtmlPipe , HtmlFilterPipe } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, viewChild, ElementRef, output, AfterViewInit, OnDestroy, input, inject} from '@angular/core';

import { BaseTabpanel, Tab } from './basetabpanel';


import { NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';


@Component({
    selector: 'default-tabpanel-active-tab-visibility-listener',
    template: '<div #element></div>',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true
})
export class DefaultTabpanelActiveTabVisibilityListener implements AfterViewInit, OnDestroy {

    readonly tab = input<Tab>(undefined as any);
    readonly visibleTab = output<Tab>();

    readonly elementRef = viewChild<ElementRef>('element');

    observer!: MutationObserver;
    log: LoggerService;

    constructor() {
        this.log = inject(LoggerFactory).getLogger('default-tabpanel');
    }

    ngAfterViewInit(): void {
        if (typeof MutationObserver !== 'undefined') {
            const tabNode = this.elementRef()!.nativeElement.parentNode.parentNode;

            this.observer = new MutationObserver((mutations) => {
                mutations.forEach((mutation) => {
                    if (mutation.attributeName === 'class') {
                        const oldValueA = mutation.oldValue ? mutation.oldValue.split(' ') : [];
                        if (oldValueA.indexOf('active') === -1 && (mutation.target as any)['classList'].contains('active')) {
                            this.visibleTab.emit(this.tab());
                        }
                    }
                });
            });

            this.observer.observe(tabNode, {
                attributes: true,
                attributeOldValue: true
            });
        } else {
            this.log.warn('MutationObserver not available, default-tabpanel may not work correctly.');
            this.visibleTab.emit(this.tab());
        }
    }

    ngOnDestroy(): void {
        if (this.observer) {
            this.observer.disconnect();
        }
    }
}

@Component( {
    selector: 'servoydefault-tabpanel',
    templateUrl: './tabpanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, HtmlFilterPipe, TooltipDirective, SabloTabseq, TrustAsHtmlPipe, NgbModule, DefaultTabpanelActiveTabVisibilityListener]
} )
export class ServoyDefaultTabpanel extends BaseTabpanel {
    
    containerStyle: Record<string, any> = { position: 'relative', overflow: 'auto' };
    height: any = '100%';
    
    private visibleTabIndex!: number;

    onTabChange( event: NgbNavChangeEvent ) {
        // do prevent it by default, so that the server side can decide of the swich can happen.
        event.preventDefault();
    }
    
    getContainerStyle(element: HTMLElement) : { [property: string]: any }{
        this.updateNavpane(element);
        if (this.servoyApi().isInAbsoluteLayout()) {
            const tabs = element.querySelector('ul');
            let calcHeight = tabs!.clientHeight;
            const clientRects = tabs!.getClientRects();
            if (clientRects && clientRects.length > 0) {
                calcHeight = tabs!.getClientRects()[0].height;
            }
            this.containerStyle['height'] = 'calc(100% - ' + calcHeight + 'px)';
            // should we set this to absolute ? it cannot be relative
            delete this.containerStyle.position;
        } else {
            if (this.height === '100%') {
                this.containerStyle['height'] = this.height;
                if (this.getNativeElement()) this.renderer.setStyle(this.getNativeElement(), 'height', '100%');
            } else {
                this.containerStyle['minHeight'] = this.height + 'px';
            }
        }
        this.containerStyle['marginTop'] = (element.offsetWidth < element.scrollWidth ? 8 : 0) + 'px';
        return this.containerStyle;
    }

    updateNavpaneTimeout: any;
    updateNavpaneTimeoutCounter: number = 0;
    private updateNavpane(element: HTMLElement) {
        if(this.updateNavpaneTimeout) {
            clearTimeout(this.updateNavpaneTimeout);
            this.updateNavpaneTimeout = null;
        }
        const navpane = element.querySelector('[ngbnavpane].show');
        if (navpane) {
            this.updateNavpaneTimeoutCounter = 0;
            if (this.height > 0) this.renderer.setStyle(navpane, 'min-height', this.height + 'px');
            else this.renderer.setStyle(navpane, 'height', '100%');
            this.renderer.setStyle(navpane, 'position', 'relative');
            if (this.height === '100%') {
            const tabs = element.querySelector('ul');
            let calcHeight = tabs!.clientHeight;
            const clientRects = tabs!.getClientRects();
            if (clientRects && clientRects.length > 0) {
                calcHeight = tabs!.getClientRects()[0].height;
            }
                this.renderer.setStyle(navpane.parentElement, 'height', 'calc(100% - ' + calcHeight + 'px)');
            }
        } else {
            if(this.updateNavpaneTimeoutCounter < 10) {
                this.updateNavpaneTimeoutCounter++;
                this.updateNavpaneTimeout = setTimeout(() => {
                    this.updateNavpane(element);
                }, 200);
            } else {
                this.log.warn('Could not find navpane in tabpanel');
            }
        }
    }
    
    tabClicked(tab: Tab, tabIndexClicked: number) {
        if (tab.disabled === true) {
            return;
        }
        this.select(this.tabs[tabIndexClicked]);
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();
        if(this.updateNavpaneTimeout) {
            clearTimeout(this.updateNavpaneTimeout);
        }
    }
    
    onVisibleTab(tab: Tab) {
        this.visibleTabIndex = this.getTabIndex(tab);
    }

    getForm(tab: Tab) {
        return this.visibleTabIndex === this.getTabIndex(tab) ? super.getForm(tab) : null;
    }
}
