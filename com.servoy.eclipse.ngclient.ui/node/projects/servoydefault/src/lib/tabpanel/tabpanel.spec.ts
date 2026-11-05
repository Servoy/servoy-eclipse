import { SimpleChanges, SimpleChange } from '@angular/core';
import { TestBed, fakeAsync, tick, waitForAsync, discardPeriodicTasks } from '@angular/core/testing';
import { ServoyDefaultTabpanel, DefaultTabpanelActiveTabVisibilityListener } from './tabpanel';
import { Tab } from './basetabpanel';

import { ServoyApi, ServoyPublicService, ServoyPublicTestingModule, LoggerFactory, WindowRefService } from '@servoy/public';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

describe( 'ServoyDefaultTabpanel', () => {
    let servoyApi;
    let mockMO;
    let observerTarget;
    let servoyPublicService;
    beforeEach( waitForAsync(() => {
        mockMO = spyOn(window, "MutationObserver");
        mockMO.and.returnValue( { observe: (param) => {observerTarget=param }, disconnect: () => { },takeRecords: () => [] } );
       
        servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId', 'formWillShow', 'hideForm', 'isInAbsoluteLayout', 'trustAsHtml','registerComponent','unRegisterComponent'] );
        servoyApi.getMarkupId.and.returnValue( '1' );
        servoyApi.isInAbsoluteLayout.and.returnValue( true );
        servoyApi.formWillShow.and.returnValue( Promise.resolve( true ) );
        servoyApi.hideForm.and.returnValue( Promise.resolve( true ) );
        servoyApi.trustAsHtml.and.returnValue(  true );
        TestBed.configureTestingModule( {
            declarations: [
                ServoyDefaultTabpanel,
                DefaultTabpanelActiveTabVisibilityListener
            ],
            imports: [NgbModule, ServoyPublicTestingModule],
            providers: [WindowRefService, LoggerFactory]
        } ).compileComponents();
        servoyPublicService = TestBed.inject(ServoyPublicService);
    } ) );

    const createComponentWithTabs = () => {
        const fixture = TestBed.createComponent( ServoyDefaultTabpanel );

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
    };
    it( 'should create the tabpanel component', waitForAsync(() => {
        const fixture = createComponentWithTabs();
        const app = fixture.debugElement.componentInstance;
        expect( app ).toBeTruthy();
    } ) );

    it( 'should select first tab and change 2 second', fakeAsync(() => {
        const fixture = createComponentWithTabs();

        let changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        
        mockMO.calls.mostRecent().args[0]([{attributeName:'class', target: observerTarget}]);
        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_0' );
        expect( fixture.componentInstance.tabIndex ).toBe( 1 );

        fixture.componentInstance.tabIndex = 2;

        changes = {};
        changes['tabIndex'] = new SimpleChange( 1, 2, false );
        fixture.componentInstance.ngOnChanges( changes );
        tick();
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    it( 'should select second  tab on index', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 2;

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true);
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        tick(250);
		discardPeriodicTasks();
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
      } ) );

    it( 'should select second  tab on name', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 'tab2';

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true);
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        tick(250);
		discardPeriodicTasks();
        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
      } ) );

    // SVY-21648 regression: resolving a tab NAME to its index must be a pure read.
    // getRealTabIndex() used to rewrite this.tabIndex to the resolved number and emit
    // tabIndexChange from inside the resolve loop. That mutation is reached on every change
    // detection pass (via getSelectedTabId()); when a tab name was assigned twice in quick
    // succession the first resolve clobbered tabIndex to a number, so the second change saw
    // a number instead of the name the script set and the panel could land on the wrong tab.
    // Resolving by name must therefore leave tabIndex untouched and emit nothing.
    it( 'should resolve a tab name to its index without mutating or emitting tabIndex (SVY-21648)', () => {
        const fixture = createComponentWithTabs();
        const component = fixture.componentInstance as any;

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.componentInstance.tabIndex = 'tab2';
        const emitSpy = spyOn( component.tabIndexChange, 'emit' );

        // getRealTabIndex is the pure resolver reached from getSelectedTabId() during rendering
        const resolved = component.getRealTabIndex();

        expect( resolved ).toBe( 1 );
        // the script-assigned name must survive the resolve, not be rewritten to the number 2
        expect( fixture.componentInstance.tabIndex ).toBe( 'tab2' );
        // and no server push may happen mid-resolve
        expect( emitSpy ).not.toHaveBeenCalled();

        // resolving again (as a quick second assignment would) still sees the name and is stable
        expect( component.getRealTabIndex() ).toBe( 1 );
        expect( fixture.componentInstance.tabIndex ).toBe( 'tab2' );
        expect( emitSpy ).not.toHaveBeenCalled();
    } );

    it( 'should apply the selected form scrollbars=never overflow to the container', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        servoyApi.isInAbsoluteLayout.and.returnValue( false );
        spyOn( servoyPublicService, 'getFormCacheByName' ).and.returnValue(
            { getBodyPartLayout: () => ({ 'overflow-x': 'hidden', 'overflow-y': 'hidden' }) } as any );

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes );
        fixture.detectChanges();
        mockMO.calls.mostRecent().args[0]([{attributeName:'class', target: observerTarget}]);
        fixture.detectChanges();

        const element = fixture.nativeElement.querySelector( '.svy-tabpanel' ) as HTMLElement;
        const style = fixture.componentInstance.getContainerStyle( element );
        tick(250);
        discardPeriodicTasks();

        expect( style['overflowX'] ).toBe( 'hidden' );
        expect( style['overflowY'] ).toBe( 'hidden' );
        expect( style['overflow'] ).toBeUndefined();
      } ) );

    it( 'should keep overflow auto when the selected form has no scrollbar restriction', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        servoyApi.isInAbsoluteLayout.and.returnValue( false );
        spyOn( servoyPublicService, 'getFormCacheByName' ).and.returnValue(
            { getBodyPartLayout: () => ({}) } as any );

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        fixture.componentInstance.ngOnChanges( changes );
        fixture.detectChanges();
        mockMO.calls.mostRecent().args[0]([{attributeName:'class', target: observerTarget}]);
        fixture.detectChanges();

        const element = fixture.nativeElement.querySelector( '.svy-tabpanel' ) as HTMLElement;
        const style = fixture.componentInstance.getContainerStyle( element );
        tick(250);
        discardPeriodicTasks();

        expect( style['overflow'] ).toBe( 'auto' );
        expect( style['overflowX'] ).toBeUndefined();
        expect( style['overflowY'] ).toBeUndefined();
      } ) );

    it( 'should not select a tab while rendering the container style', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        servoyApi.isInAbsoluteLayout.and.returnValue( false );
        spyOn( servoyPublicService, 'getFormCacheByName' ).and.returnValue(
            { getBodyPartLayout: () => ({}) } as any );

        const element = document.createElement( 'div' );
        element.appendChild( document.createElement( 'ul' ) );
        fixture.componentInstance.getContainerStyle( element );
        tick(250);
        discardPeriodicTasks();

        // reading the container style must be side effect free: no tab may be selected yet
        expect( fixture.componentInstance.getSelectedTab() ).toBeUndefined();
        expect( servoyApi.formWillShow ).not.toHaveBeenCalled();
      } ) );

    it( 'should honour tabIndex when the container style was rendered first', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        servoyApi.isInAbsoluteLayout.and.returnValue( false );
        spyOn( servoyPublicService, 'getFormCacheByName' ).and.returnValue(
            { getBodyPartLayout: () => ({}) } as any );
        fixture.componentInstance.tabIndex = 2;

        const element = document.createElement( 'div' );
        element.appendChild( document.createElement( 'ul' ) );
        fixture.componentInstance.getContainerStyle( element );

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes );
        fixture.detectChanges();
        tick(250);
        discardPeriodicTasks();

        expect( fixture.componentInstance.getSelectedTabId() ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.selectedTabID ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
      } ) );

} );
