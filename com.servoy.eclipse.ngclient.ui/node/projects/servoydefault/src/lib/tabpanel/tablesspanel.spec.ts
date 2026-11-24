import { SimpleChanges, SimpleChange } from '@angular/core';
import { TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultTablesspanel } from './tablesspanel';
import { Tab } from './basetabpanel';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';


import { ServoyPublicTestingModule, LoggerFactory, WindowRefService, ServoyApi } from '@servoy/public';

describe( 'ServoyDefaultTabpanel', () => {
    let servoyApi;
    beforeEach( () => {
        servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId', 'formWillShow', 'hideForm','registerComponent','unRegisterComponent'] );
        servoyApi.getMarkupId.and.returnValue( '1' );
        servoyApi.formWillShow.and.returnValue( Promise.resolve( true ) );
        servoyApi.hideForm.and.returnValue( Promise.resolve( true ) );
        TestBed.configureTestingModule( {
            declarations: [
                ServoyDefaultTablesspanel
            ],
            imports: [NgbModule, ServoyPublicTestingModule],
            providers: [WindowRefService, LoggerFactory]
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
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_0' );
        expect( fixture.componentInstance.tabIndex ).toBe( 1 );

        fixture.componentInstance.tabIndex = 2;

        changes = {};
        changes['tabIndex'] = new SimpleChange( 1, 2, false );
        fixture.componentInstance.ngOnChanges( changes );
        tick();
        expect( fixture.componentInstance.getSelectedTab()._id).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    it( 'should select second  tab on index', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 2;

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    it( 'should select second  tab on name', fakeAsync(() => {
        const fixture = createComponentWithTabs();
        fixture.componentInstance.tabIndex = 'tab2';

        const changes: SimpleChanges = {};
        changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
        changes['tabIndex'] = new SimpleChange( null, 2, true );
        fixture.componentInstance.ngOnChanges( changes );

        fixture.detectChanges();
        expect( fixture.componentInstance.getSelectedTab()._id ).toBe( '1_tab_1' );
        expect( fixture.componentInstance.tabIndex ).toBe( 2 );
    } ) );

    // SVY-21792: borderType must be applied (through PropertyUtils.setBorder) on the single #element div
    describe( 'borderType (SVY-21792)', () => {
        const matte = { type: 'MatteBorder', borderStyle: { borderWidth: '1px 2px 3px 4px ', borderStyle: 'solid', borderColor: 'red' } };
        const line = { type: 'LineBorder', borderStyle: { borderWidth: '2px', borderStyle: 'solid', borderColor: 'blue' } };
        const sides = ['Top', 'Right', 'Bottom', 'Left'];

        const changeBorder = ( fixture, oldValue: any, newValue: any, first = false ) => {
            fixture.componentInstance.borderType = newValue;
            const changes: SimpleChanges = {};
            changes['borderType'] = new SimpleChange( oldValue, newValue, first );
            fixture.componentInstance.ngOnChanges( changes );
        };
        const renderWithBorder = ( fixture, borderType: any ) => {
            const changes: SimpleChanges = {};
            changes['tabs'] = new SimpleChange( null, fixture.componentInstance.tabs, true );
            fixture.componentInstance.ngOnChanges( changes );
            fixture.detectChanges();
            changeBorder( fixture, null, borderType, true );
        };
        const outer = ( fixture ) => fixture.nativeElement.querySelector( '.svy-tablesspanel' ) as HTMLElement;

        it( 'should apply a Matte border with a width per side', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, matte );
            fixture.detectChanges();

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

        it( 'should apply a Line border on all sides and keep it after change detection', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, line );
            // the ngStyle binding on the same element must not wipe the border
            fixture.componentInstance.detectChanges();
            fixture.detectChanges();

            const style = outer( fixture ).style;
            sides.forEach( s => {
                expect( style['border' + s + 'Width'] ).toBe( '2px' );
                expect( style['border' + s + 'Style'] ).toBe( 'solid' );
                expect( style['border' + s + 'Color'] ).toBe( 'blue' );
            } );
        } ) );

        [null, undefined].forEach( empty => {
            it( 'should not draw a border when borderType is ' + empty, fakeAsync(() => {
                const fixture = createComponentWithTabs();
                renderWithBorder( fixture, empty );

                const style = outer( fixture ).style;
                sides.forEach( s => expect( style['border' + s + 'Width'] ).toBe( '' ) );
            } ) );
        } );

        it( 'should remove a previously applied border when borderType changes to null', fakeAsync(() => {
            const fixture = createComponentWithTabs();
            renderWithBorder( fixture, line );
            expect( outer( fixture ).style.borderTopWidth ).toBe( '2px' );

            changeBorder( fixture, line, null );

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

            const style = outer( fixture ).style;
            expect( style.borderTopWidth ).toBe( '1px' );
            expect( style.borderRightWidth ).toBe( '2px' );
            expect( style.borderBottomWidth ).toBe( '3px' );
            expect( style.borderLeftWidth ).toBe( '4px' );
        } ) );

        it( 'should keep the legacy borderStyle shorthand in the container style and the overflow', () => {
            const fixture = createComponentWithTabs();
            fixture.componentInstance.borderType = line;

            const style = fixture.componentInstance.getContainerStyle();

            expect( style['border'] ).toBe( '2px solid blue' );
            expect( style['overflow'] ).toBe( 'auto' );
        } );
    } );

} );

