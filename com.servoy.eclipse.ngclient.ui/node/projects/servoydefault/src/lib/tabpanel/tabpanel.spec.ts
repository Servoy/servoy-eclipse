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

    // SVY-21792: borderType must be applied (through PropertyUtils.setBorder) on the outer .svy-tabpanel
    describe( 'borderType (SVY-21792)', () => {
        const matte = { type: 'MatteBorder', borderStyle: { borderWidth: '1px 2px 3px 4px ', borderStyle: 'solid', borderColor: 'red' } };
        const line = { type: 'LineBorder', borderStyle: { borderWidth: '2px', borderStyle: 'solid', borderColor: 'blue' } };

        const renderWithBorder = ( fixture, borderType: any ) => {
            const changes: SimpleChanges = {};
            changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
            fixture.componentInstance.ngOnChanges( changes );
            fixture.detectChanges();
            fixture.componentInstance.borderType = borderType;
            changeBorder( fixture, null, borderType, true );
        };
        const changeBorder = ( fixture, oldValue: any, newValue: any, first = false ) => {
            fixture.componentInstance.borderType = newValue;
            const changes: SimpleChanges = {};
            changes['borderType'] = new SimpleChange( oldValue, newValue, first );
            fixture.componentInstance.ngOnChanges( changes );
        };
        const outer = ( fixture ) => fixture.nativeElement.querySelector( '.svy-tabpanel' ) as HTMLElement;
        const sides = ['Top', 'Right', 'Bottom', 'Left'];

        it( 'should apply a Matte border with a width per side on the outer element', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, matte );
            discardPeriodicTasks();

            const style = outer( fixture ).style;
            expect( style.borderTopWidth ).toBe( '1px' );
            expect( style.borderRightWidth ).toBe( '2px' );
            expect( style.borderBottomWidth ).toBe( '3px' );
            expect( style.borderLeftWidth ).toBe( '4px' );
            sides.forEach( s => {
                expect( style['border' + s + 'Style'] ).toBe( 'solid' );
                expect( style['border' + s + 'Color'] ).toBe( 'red' );
            } );
            const computed = getComputedStyle( outer( fixture ) );
            expect( computed.borderTopWidth ).toBe( '1px' );
            expect( computed.borderLeftWidth ).toBe( '4px' );
        } ) );

        it( 'should apply a Line border on all sides', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, line );
            discardPeriodicTasks();

            const style = outer( fixture ).style;
            sides.forEach( s => {
                expect( style['border' + s + 'Width'] ).toBe( '2px' );
                expect( style['border' + s + 'Style'] ).toBe( 'solid' );
                expect( style['border' + s + 'Color'] ).toBe( 'blue' );
            } );
        } ) );

        it( 'should not put the border on the tab strip or the content outlet', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, matte );
            discardPeriodicTasks();

            const ul = outer( fixture ).querySelector( 'ul' ) as HTMLElement;
            const outlet = outer( fixture ).querySelector( '[ngbNavOutlet], .tab-content' ) as HTMLElement;
            expect( ul ).toBeTruthy();
            expect( ul.style.borderTopWidth ).toBe( '' );
            expect( ul.style.borderLeftWidth ).toBe( '' );
            if ( outlet ) {
                expect( outlet.style.borderTopWidth ).toBe( '' );
            }
            expect( ( outer( fixture ).lastElementChild as HTMLElement ).style.borderTopWidth ).toBe( '' );
        } ) );

        [null, undefined].forEach( empty => {
            it( 'should not draw a border when borderType is ' + empty, fakeAsync(() => {
                const fixture = createComponentWithTabs();
                renderWithBorder( fixture, empty );
                discardPeriodicTasks();

                const style = outer( fixture ).style;
                sides.forEach( s => expect( style['border' + s + 'Width'] ).toBe( '' ) );
            } ) );
        } );

        it( 'should remove a previously applied border when borderType changes to null', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, line );
            expect( outer( fixture ).style.borderTopWidth ).toBe( '2px' );

            changeBorder( fixture, line, null );
            discardPeriodicTasks();

            const style = outer( fixture ).style;
            sides.forEach( s => {
                expect( style['border' + s + 'Width'] ).toBe( '' );
                expect( style['border' + s + 'Style'] ).toBe( '' );
            } );
        } ) );

        it( 'should update the widths when borderType changes from Matte to Line', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, matte );
            expect( outer( fixture ).style.borderLeftWidth ).toBe( '4px' );

            changeBorder( fixture, matte, line );
            discardPeriodicTasks();

            const style = outer( fixture ).style;
            sides.forEach( s => {
                expect( style['border' + s + 'Width'] ).toBe( '2px' );
                expect( style['border' + s + 'Color'] ).toBe( 'blue' );
            } );
        } ) );

        it( 'should not create a fieldset or move children for a TitledBorder and clear an earlier border', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, line );
            const before = Array.from( outer( fixture ).childNodes );
            expect( outer( fixture ).style.borderTopWidth ).toBe( '2px' );

            const titled = { type: 'TitledBorder', title: 'a title', color: 'red', titleJustification: 'left' };
            changeBorder( fixture, line, titled );
            discardPeriodicTasks();

            expect( outer( fixture ).querySelector( 'fieldset' ) ).toBeNull();
            expect( Array.from( outer( fixture ).childNodes ) ).toEqual( before );
            expect( outer( fixture ).style.borderTopWidth ).toBe( '' );
        } ) );

        it( 'should apply a borderType that arrives before the view exists once the view is initialized', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            fixture.componentInstance.borderType = matte;
            const changes: SimpleChanges = {};
            changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
            changes['borderType'] = new SimpleChange( null, matte, true );
            fixture.componentInstance.ngOnChanges( changes );

            // no #element yet, so nothing could have been applied
            expect( fixture.componentInstance.getNativeElement() ).toBeFalsy();

            fixture.detectChanges();
            discardPeriodicTasks();

            const style = outer( fixture ).style;
            expect( style.borderTopWidth ).toBe( '1px' );
            expect( style.borderRightWidth ).toBe( '2px' );
            expect( style.borderBottomWidth ).toBe( '3px' );
            expect( style.borderLeftWidth ).toBe( '4px' );
        } ) );
    } );

} );

