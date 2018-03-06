import { TestBed, inject } from '@angular/core/testing';


import { ConverterService } from '../../sablo/converter.service';

import { SpecTypesService, BaseCustomObject } from '../../sablo/spectypes.service'

import { JSONObjectConverter } from './json_object_converter';

describe( 'JSONObjectConverter', () => {
    // copy from JSONObjectConverter
    const UPDATES = "u";
    const KEY = "k";
    const INITIALIZE = "in";
    const VALUE = "v";
    const PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    const CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    const NO_OP = "n";
    const REAL_TYPE = "rt";

    let converterService: ConverterService
    beforeEach(() => {
        TestBed.configureTestingModule( {
            providers: [ConverterService, SpecTypesService]
        } );
        converterService = TestBed.get( ConverterService );
        converterService.registerCustomPropertyHandler( "JSON_obj", new JSONObjectConverter( converterService ) );
        converterService.getSpecTypesService().registerType( "Tab", Tab, ["name", "myvalue"] );
        converterService.getSpecTypesService().registerType( "TabHolder", TabHolder, ["id", "tab", "tab2"] );
    } );

    function createTabJSON() {
        const data = { name: "test", myvalue: "test" };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "Tab";
        json[CONTENT_VERSION] = 1;
        return json;
    }
    function createTabObjectFromServer(): Tab {

        return converterService.convertFromServerToClient( createTabJSON(), "JSON_obj" );;
    }

    it( 'type should be created and of the right type', () => {
        const tab = converterService.getSpecTypesService().createType( "Tab" );
        expect( tab ).toBeDefined();
        expect( tab instanceof Tab ).toBeTruthy();
    } );

    it( 'object should be created changed from server side', () => {
        let tab: Tab = createTabObjectFromServer();
        expect( tab ).toBeDefined();
        expect( tab instanceof Tab ).toBeTruthy( "tab should be instance of Tab" );
        expect( tab.name ).toBe( "test", "name should be test" );
        expect( tab.myvalue ).toBe( "test", "myvalue should be test" );
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );

        const update = {};
        update[KEY] = "myvalue";
        update[VALUE] = "test2";
        const json = {};
        json[UPDATES] = [update];
        json[CONTENT_VERSION] = 1;
        tab = converterService.convertFromServerToClient( json, "JSON_obj", tab );
        expect( tab.myvalue ).toBe( "test2", "myvalue should be test2" );
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );
    } );

    it( 'object created and changed from client side', () => {
        let tab: Tab = createTabObjectFromServer();
        tab.name = "test2";
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes because name is not monitored" );
        tab.myvalue = "test";
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes because of same assignment" );
        tab.myvalue = "test2";
        expect( tab.getStateHolder().getChanges().length ).toBe( 1, "should have changes because of  assignment" );
        const changes = converterService.convertFromClientToServer( tab, "JSON_obj" );
        expect( changes ).toBeDefined( "change object should be generated" );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE] ).toBe( "test2" );
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );
    } );

    it( 'create object in scripting', () => {
        const tab = new Tab();
        tab.name = "test";
        tab.myvalue = "test2";
        const changes = converterService.convertFromClientToServer( tab, "JSON_obj" );
        expect( changes ).toBeDefined( "change object should be generated" );
        expect( changes[CONTENT_VERSION] ).toBeUndefined( "should not have a content version" );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have a value" );
        expect( changes[VALUE].name ).toBe( "test" );
        expect( changes[VALUE].myvalue ).toBe( "test2" );
        expect( tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );

    } );

    it( 'nested object created in scripting', () => {
        const tabHolder = new TabHolder();
        tabHolder.id = "test";
        tabHolder.tab = new Tab();
        tabHolder.tab.name = "test";
        tabHolder.tab.myvalue = "test";
        expect( tabHolder.getStateHolder().allChanged ).toBe( true, "should not have changes" );
        const changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[VALUE] ).toBeDefined( "change object should have a value" );
        expect( changes[VALUE].id ).toBe( "test" );
        expect( changes[VALUE].tab[VALUE] ).toBeDefined();
        expect( changes[VALUE].tab[VALUE].name ).toBe( "test" );
        expect( changes[VALUE].tab[VALUE].myvalue ).toBe( "test" );
        expect( tabHolder.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );;
        expect( tabHolder.tab.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );

        tabHolder.tab.name = "test2";
        expect( tabHolder.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );;
        tabHolder.tab.myvalue = "test2";
        expect(tabHolder.tab.getStateHolder().getChanges().length).toBe(1, "should have changes");;
        //          expect(tabHolder.getStateHolder().getChanges().length).toBe(1, "should have changes");;

    } );

    it( 'nested object from server', () => {
        const data = { id: "test", tab2: createTabJSON() };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "TabHolder";
        json[CONTENT_VERSION] = 1;
        json[ConverterService.TYPES_KEY] = { "tab": "JSON_obj", "tab2": "JSON_obj" };
        const tabHolder: TabHolder = converterService.convertFromServerToClient( json, "JSON_obj" );

        expect( tabHolder.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );

        tabHolder.tab2.name = "test2";
        expect( tabHolder.getStateHolder().getChanges().length ).toBe( 0, "should not have changes" );
        tabHolder.tab2.myvalue = "test2";
        expect( tabHolder.getStateHolder().getChanges().length ).toBe( 1, "should have changes" );

        const changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[UPDATES] ).toBeDefined( "change object  should have an update value" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tab2" );
        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined( "change object  should have an update value" );
        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1, "should have 1 update" );

        expect( changes[UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( "test2" );
    } );
} );

class Tab extends BaseCustomObject {
    name: string

    private _myvalue: string;

    get myvalue(): string {
        return this._myvalue;
    }

    set myvalue( value: string ) {
        this.getStateHolder().markIfChanged( "myvalue", value, this._myvalue );
        this._myvalue = value;
    }
}

class TabHolder extends BaseCustomObject {
    id: string;

    tab: Tab;

    _tab2: Tab;

    get tab2() { return this._tab2 }

    set tab2( tab: Tab ) {
        this.getStateHolder().markIfChanged( "myvalue", tab, this._tab2 );
        this._tab2 = tab;
    }
}

