import { describe, it, expect, beforeEach, vi } from 'vitest';
import { SimpleChanges, SimpleChange } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { ServoyDefaultTablesspanel } from './tablesspanel';
import { Tab } from './basetabpanel';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';


import { LoggerFactory, WindowRefService, ServoyApi, ServoyPublicService,
         SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';

describe( 'ServoyDefaultTabpanel', () => {
    let servoyApi: any;
    beforeEach( () => {
        servoyApi = { getMarkupId: vi.fn(), formWillShow: vi.fn(), hideForm: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
        servoyApi.getMarkupId.mockReturnValue( '1' );
        servoyApi.formWillShow.mockReturnValue( Promise.resolve( true ) );
        servoyApi.hideForm.mockReturnValue( Promise.resolve( true ) );
        TestBed.configureTestingModule( {
            imports: [ServoyDefaultTablesspanel, SabloTabseq, NgbModule, CommonModule],
      providers: [WindowRefService, LoggerFactory,
                        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
        } ).compileComponents();
    } );

    const createComponentWithTabs = () => {
        const fixture = TestBed.createComponent( ServoyDefaultTablesspanel );

        fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

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
    }
    it( 'should create the tabpanel component', async () => {
        const fixture = createComponentWithTabs();
        const app = fixture.debugElement.componentInstance;
        expect( app ).toBeTruthy();
    } );

    it( 'should select first tab and change 2 second', async () => {
        vi.useFakeTimers();
        const fixture = createComponentWithTabs();

        let changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        await vi.advanceTimersByTimeAsync(0);
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_0' );
        expect( fixture.componentInstance.tabIndex ).toBe( 1 );

        fixture.componentInstance.tabIndex = 2;

        changes = {};
        changes['tabIndex'] = new SimpleChange( 1, 2, false );
        fixture.componentInstance.ngOnChanges( changes );
        await vi.advanceTimersByTimeAsync(0);
        expect( fixture.componentInstance.getSelectedTab()._id).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
        vi.useRealTimers();
    } );

    it( 'should select second  tab on index', async () => {
        vi.useFakeTimers();
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 2;

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        await vi.advanceTimersByTimeAsync(0);
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
        vi.useRealTimers();
    } );

    it( 'should select second  tab on name', async () => {
        vi.useFakeTimers();
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 'tab2';

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        await vi.advanceTimersByTimeAsync(0);
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
        vi.useRealTimers();
    } );

} );
