import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SimpleChanges, SimpleChange } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ServoyDefaultTabpanel, DefaultTabpanelActiveTabVisibilityListener } from './tabpanel';
import { Tab } from './basetabpanel';

import { LoggerFactory, WindowRefService, ServoyPublicService, ServoyApi,
         SabloTabseq, TooltipDirective, TooltipService, HtmlFilterPipe, TrustAsHtmlPipe } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

describe( 'ServoyDefaultTabpanel', () => {
    let servoyApi: any;
    let mockMOCallback: any;
    let observerTarget: any;
    beforeEach( async () => {
        (window as any).MutationObserver = class {
            constructor(cb: any) { mockMOCallback = cb; }
            observe(target: any) { observerTarget = target; }
            disconnect() {}
            takeRecords() { return []; }
        };

        servoyApi = { getMarkupId: vi.fn(), formWillShow: vi.fn(), hideForm: vi.fn(), isInAbsoluteLayout: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
        servoyApi.getMarkupId.mockReturnValue( '1' );
        servoyApi.isInAbsoluteLayout.mockReturnValue( true );
        servoyApi.formWillShow.mockReturnValue( Promise.resolve( true ) );
        servoyApi.hideForm.mockReturnValue( Promise.resolve( true ) );
        servoyApi.trustAsHtml.mockReturnValue(  true );
        TestBed.configureTestingModule( {
            declarations: [ServoyDefaultTabpanel, DefaultTabpanelActiveTabVisibilityListener,
                           SabloTabseq, TooltipDirective, HtmlFilterPipe, TrustAsHtmlPipe],
            imports: [NgbModule],
            providers: [WindowRefService, LoggerFactory, TooltipService,
                        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
        } ).compileComponents();
    } );

    const createComponentWithTabs = () => {
        const fixture = TestBed.createComponent( ServoyDefaultTabpanel );
        fixture.componentInstance.servoyApi = servoyApi;

        const tabs = [];
        let tab = new Tab();
        tab.name = 'tab1';
        tab.containsFormId = 'form1';
        tab.text = 'tab1';
        tabs[0] = tab;
        tab = new Tab();
        tab.name = 'tab2';
        tab.containsFormId = 'form2';
        tab.text = 'tab2';
        tabs[1] = tab;

        fixture.componentInstance.tabs = tabs;
        return fixture;
    };

    it( 'should create the tabpanel component', () => {
        const fixture = createComponentWithTabs();
        expect( fixture.componentInstance ).toBeTruthy();
    } );

    it( 'should select first tab and change 2 second', async () => {
        const fixture = createComponentWithTabs();

        let changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.svyOnChanges( changes );

        fixture.detectChanges();

        mockMOCallback([{attributeName:'class', target: observerTarget}]);
        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_0' );
        expect( fixture.componentInstance.tabIndex ).toBe( 1 );

        fixture.componentInstance.tabIndex = 2;

        changes = {};
        changes['tabIndex'] = new SimpleChange( 1, 2, false );
        fixture.componentInstance.svyOnChanges( changes );
        await new Promise(resolve => setTimeout(resolve, 0));
        await new Promise(resolve => setTimeout(resolve, 0));
        await new Promise(resolve => setTimeout(resolve, 0));
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } );

    it( 'should select second  tab on index', async () => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 2;

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true);
        fixture.componentInstance.svyOnChanges( changes );

        fixture.detectChanges();
        await new Promise(resolve => setTimeout(resolve, 0));
        await new Promise(resolve => setTimeout(resolve, 0));
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
      } );

    it( 'should select second  tab on name', async () => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 'tab2';

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true);
        fixture.componentInstance.svyOnChanges( changes );

        fixture.detectChanges();
        await new Promise(resolve => setTimeout(resolve, 0));
        await new Promise(resolve => setTimeout(resolve, 0));
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
      } );
} );
