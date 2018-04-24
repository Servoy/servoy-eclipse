import { TestBed, inject } from '@angular/core/testing';
import { IterableDiffers } from '@angular/core';

import { ConverterService } from '../../sablo/converter.service';
import { LoggerService } from '../../sablo/logger.service'

import { SpecTypesService, BaseCustomObject, ICustomArray } from '../../sablo/spectypes.service'

import { JSONArrayConverter } from './json_array_converter';
import { JSONObjectConverter } from './json_object_converter';

describe( 'JSONArrayConverter', () => {
    // copied from JSONArrayConverter
    const UPDATES = "u";
    const REMOVES = "r";
    const ADDITIONS = "a";
    const INDEX = "i";
    const INITIALIZE = "in";
    const VALUE = "v";
    const PUSH_TO_SERVER = "w"; // value is undefined when we shouldn't send changes to server, false if it should be shallow watched and true if it should be deep watched
    const CONTENT_VERSION = "vEr"; // server side sync to make sure we don't end up granular updating something that has changed meanwhile server-side
    const NO_OP = "n";

    // copied from JSONObjectConverter
    const REAL_TYPE = "rt";
    const KEY = "k";

    let converterService: ConverterService
    beforeEach(() => {
        TestBed.configureTestingModule( {
            providers: [ConverterService, SpecTypesService,LoggerService]
        } );
        const specTypes = TestBed.get( SpecTypesService );
        converterService = TestBed.get( ConverterService );
        converterService.registerCustomPropertyHandler( "JSON_obj", new JSONObjectConverter( converterService, specTypes ) );
        converterService.registerCustomPropertyHandler( "JSON_arr", new JSONArrayConverter( converterService, specTypes, TestBed.get( IterableDiffers ) ) );
        TestBed.get( SpecTypesService ).registerType( "Tab", Tab, ["name", "myvalue"] );
        TestBed.get( SpecTypesService ).registerType( "TabHolder", TabHolder, ["name", "tabs"] );
    } );


    function createDefaultArrayJSON() {
        const data = ["test1", "test2", "test3"]
        const json = {};
        json[VALUE] = data;
        json[CONTENT_VERSION] = 1;
        return json;
    }

    function createTabJSON( val ) {
        const data = { name: val, myvalue: val };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "Tab";
        json[CONTENT_VERSION] = 1;
        return json;
    }

    function createTabHolderJSONWithFilledArray( val ) {
        const data = { name: val, tabs: createtArrayWithJSONObjet() };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "TabHolder";
        json[CONTENT_VERSION] = 1;
        json[ConverterService.TYPES_KEY] = { tabs: "JSON_arr" }
        return json;
    }

    function createTabHolderJSON( val ) {
        const data = { name: val };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "TabHolder";
        json[CONTENT_VERSION] = 1;
        json[ConverterService.TYPES_KEY] = { tabs: "JSON_arr" }
        return json;
    }

    function createtArrayWithJSONObjet() {
        const data = [createTabJSON( "test1" ), createTabJSON( "test2" ), createTabJSON( "test3" )]
        const json = {};
        json[VALUE] = data;
        json[CONTENT_VERSION] = 1;
        json[ConverterService.TYPES_KEY] = ["JSON_obj", "JSON_obj", "JSON_obj"]
        return json;
    }

    it( 'type should be created an array from server to client', () => {
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        expect( val ).toBeDefined();
        expect( val.length ).toBe( 3, "array length should be 3" );
        expect( val[0] ).toBe( "test1", "array[0] should be test1" );
        expect( val[1] ).toBe( "test2", "array[1] should be  test2" );
        expect( val[2] ).toBe( "test3", "array[2] should be  test3" );
    } );

    it( 'simple change of 1 index', () => {
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        val[0] = "test4"
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][INDEX] ).toBe( 0 );
        expect( changes[UPDATES][0][VALUE] ).toBe( "test4" );

        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
    } );

    it( 'remove of 1 index', () => {
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        val.splice( 1, 1 );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[VALUE].length ).toBe( 2, "should have 2 updates" );
        expect( changes[VALUE][0] ).toBe( 'test1' );
        expect( changes[VALUE][1] ).toBe( "test3" );

        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
    } );

    it( 'add  of 1 index', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        val[3] = "test4";
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[VALUE].length ).toBe( 4, "should have 4 updates" );
        expect( changes[VALUE][0] ).toBe( 'test1' );
        expect( changes[VALUE][1] ).toBe( 'test2' );
        expect( changes[VALUE][2] ).toBe( "test3" );
        expect( changes[VALUE][3] ).toBe( 'test4' );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'remove of 1 index and add of one', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        val.splice( 1, 1 );
        val[2] = "test4";
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 2, "should have 2 updates" );
        expect( changes[UPDATES][0][INDEX] ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE] ).toBe( "test3" );
        expect( changes[UPDATES][1][INDEX] ).toBe( 2 );
        expect( changes[UPDATES][1][VALUE] ).toBe( "test4" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'remove change and add of one', () => {
        // TODO this could be improved by really only sending inserts/removes of indexes
        const val: Array<string> = converterService.convertFromServerToClient( createDefaultArrayJSON(), "JSON_arr" )
        val.splice( 0, 1 );
        val[1] = "test4";
        val[2] = "test5";
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 3, "should have 3 updates" );
        expect( changes[UPDATES][0][INDEX] ).toBe( 0 );
        expect( changes[UPDATES][0][VALUE] ).toBe( "test2" );
        expect( changes[UPDATES][1][INDEX] ).toBe( 1 );
        expect( changes[UPDATES][1][VALUE] ).toBe( "test4" );
        expect( changes[UPDATES][2][INDEX] ).toBe( 2 );
        expect( changes[UPDATES][2][VALUE] ).toBe( "test5" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'type should be created an array from server to client with custom json objects', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )
        expect( val ).toBeDefined();
        expect( val.length ).toBe( 3, "array length should be 3" );
        expect( val[0].name ).toBe( "test1", "array[0] should be tab.name = test1" );
        expect( val[1].name ).toBe( "test2", "array[1] should be tab.name = . test2" );
        expect( val[2].name ).toBe( "test3", "array[2] should be  tab.name = .test3" );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( ' change of one tab value', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )
        val[0].name = "test4";
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeTruthy();

        val[0].myvalue = "test4";
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][INDEX] ).toBe( 0 );
        expect( changes[UPDATES][0][VALUE][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1, "should have 1 update" );

        expect( changes[UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( "test4" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'delete 1 tab', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )

        val.splice( 1, 1 );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[VALUE].length ).toBe( 2, "should have 2 values" );
        expect( changes[VALUE][0][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE][0][VALUE].name ).toBe( "test1", "item[0].name should be test1" );
        expect( changes[VALUE][0][VALUE].myvalue ).toBe( "test1", "item[0].myvalue should be test1" );

        expect( changes[VALUE][1][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE][1][VALUE].name ).toBe( "test3", "item[1].name should be test3" );
        expect( changes[VALUE][1][VALUE].myvalue ).toBe( "test3", "item[1].myvalue should be test3" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'add 1 tab', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )

        const addedTab = new Tab();
        addedTab.myvalue = "test5"
        addedTab.name = "test5"
        val.push( addedTab );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[VALUE].length ).toBe( 4, "should have 4 values" );
        expect( changes[VALUE][0][CONTENT_VERSION] ).toBe( 1 );

        expect( changes[VALUE][3][CONTENT_VERSION] ).toBeUndefined();
        expect( changes[VALUE][3][VALUE].name ).toBe( "test5", "item[3].name should be test5" );
        expect( changes[VALUE][3][VALUE].myvalue ).toBe( "test5", "item[3].myvalue should be test5" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( ' change of one tab value delete 1 other', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )

        val[0].myvalue = "test4";
        val.splice( 1, 1 );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[VALUE].length ).toBe( 2, "should have 2 values" );
        expect( changes[VALUE][0][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE][0][VALUE].name ).toBe( "test1", "item[0].name should be test1" );
        expect( changes[VALUE][0][VALUE].myvalue ).toBe( "test4", "item[0].myvalue should be test4" );

        expect( changes[VALUE][1][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[VALUE][1][VALUE].name ).toBe( "test3", "item[1].name should be test3" );
        expect( changes[VALUE][1][VALUE].myvalue ).toBe( "test3", "item[1].myvalue should be test3" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( ' delete and add one', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )

        val.splice( 1, 1 );
        const addedTab = new Tab();
        addedTab.myvalue = "test5"
        addedTab.name = "test5"
        val.push( addedTab );

        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 2, "should have 2 update" );

        expect( changes[UPDATES][0][INDEX] ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE] ).toBeDefined( "change object  shoulld have value" );
        expect( changes[UPDATES][0][VALUE][VALUE].name ).toBe( "test3" );
        expect( changes[UPDATES][0][VALUE][VALUE].myvalue ).toBe( "test3" );

        expect( changes[UPDATES][1][INDEX] ).toBe( 2 );
        expect( changes[UPDATES][1][VALUE] ).toBeDefined( "change object  shoulld have value" );
        expect( changes[UPDATES][1][VALUE][VALUE].name ).toBe( "test5" );
        expect( changes[UPDATES][1][VALUE][VALUE].myvalue ).toBe( "test5" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );


    } );

    it( ' change of one tab value delete 1 other and add 1', () => {
        const val: Array<Tab> = converterService.convertFromServerToClient( createtArrayWithJSONObjet(), "JSON_arr" )

        val[0].myvalue = "test4";
        val.splice( 1, 1 );
        const addedTab = new Tab();
        addedTab.myvalue = "test5"
        addedTab.name = "test5"
        val.push( addedTab );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 3, "should have 1 update" );
        expect( changes[UPDATES][0][INDEX] ).toBe( 0 );
        expect( changes[UPDATES][0][VALUE][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( "test4" );

        expect( changes[UPDATES][1][INDEX] ).toBe( 1 );
        expect( changes[UPDATES][1][VALUE] ).toBeDefined( "change object  shoulld have value" );
        expect( changes[UPDATES][1][VALUE][VALUE].name ).toBe( "test3" );
        expect( changes[UPDATES][1][VALUE][VALUE].myvalue ).toBe( "test3" );

        expect( changes[UPDATES][2][INDEX] ).toBe( 2 );
        expect( changes[UPDATES][2][VALUE] ).toBeDefined( "change object  shoulld have value" );
        expect( changes[UPDATES][2][VALUE][VALUE].name ).toBe( "test5" );
        expect( changes[UPDATES][2][VALUE][VALUE].myvalue ).toBe( "test5" );
        changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
    } );

    it( 'create a tabholder wiht 3 tabs in its array', () => {
        const val: TabHolder = converterService.convertFromServerToClient( createTabHolderJSONWithFilledArray( "test" ), "JSON_obj" )
        expect( val.tabs.length ).toBe( 3, "should have 3 tabs" );
        let changes = converterService.convertFromClientToServer( val, "JSON_arr", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
    } );

    it( 'update a tab in the tabs array of the TabHolder', () => {
        const val: TabHolder = converterService.convertFromServerToClient( createTabHolderJSONWithFilledArray( "test" ), "JSON_obj" )
        val.tabs[0].myvalue = "test4";
        let changes = converterService.convertFromClientToServer( val, "JSON_obj", val );

        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tabs", "should have tabs key" );
        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined();
        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][INDEX] ).toBe( 0 );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE][UPDATES].length ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( "test4" );
        changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } );

    it( 'add script tabs array into a TabHolder', () => {
        const val: TabHolder = converterService.convertFromServerToClient( createTabHolderJSONWithFilledArray( "test" ), "JSON_obj" )
        const tabs: Array<Tab> = []
        tabs[0] = new Tab();
        tabs[0].name = "test1";
        tabs[0].myvalue = "test1";
        val.tabs = tabs;
        let changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tabs", "should have tabs key" );
        expect( changes[UPDATES][0][VALUE][VALUE] ).toBeDefined();
        expect( changes[UPDATES][0][VALUE][VALUE].length ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][VALUE][0][VALUE].name ).toBe( "test1" );
        expect( changes[UPDATES][0][VALUE][VALUE][0][VALUE].myvalue ).toBe( "test1" );
        changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );

    } )

    it( 'test mark for change', () => {
        const val: TabHolder = converterService.convertFromServerToClient( createTabHolderJSONWithFilledArray( "test" ), "JSON_obj" )
        val.tabs[3] = new Tab();
        val.tabs[3].name = "test4";
        val.tabs[3].myvalue = "test4";
        let changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
        const tabs = val.tabs as ICustomArray<Tab>;
        tabs.markForChanged();
        changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        console.log( changes )
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tabs", "should have tabs key" );
        expect( changes[UPDATES][0][VALUE][VALUE] ).toBeDefined();
        expect( changes[UPDATES][0][VALUE][VALUE].length ).toBe( 4 );
        expect( changes[UPDATES][0][VALUE][VALUE][0][VALUE].name ).toBe( "test1" );
        expect( changes[UPDATES][0][VALUE][VALUE][0][VALUE].myvalue ).toBe( "test1" );
        expect( changes[UPDATES][0][VALUE][VALUE][0][CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES][0][VALUE][VALUE][3][VALUE].name ).toBe( "test4" );
        expect( changes[UPDATES][0][VALUE][VALUE][3][VALUE].myvalue ).toBe( "test4" );
        expect( changes[UPDATES][0][VALUE][VALUE][3][CONTENT_VERSION] ).toBeUndefined();
        changes = converterService.convertFromClientToServer( val, "JSON_obj", val );
        expect( changes[NO_OP] ).toBeDefined( "should have no changes now" );
    } );;
} );

class Tab extends BaseCustomObject {
    name: string

    private _myvalue: string;

    get myvalue(): string {
        return this._myvalue;
    }

    set myvalue( value: string ) {
        this.getStateHolder().setPropertyAndHandleChanges( this, "_myvalue", "myvalue", value );
    }
}

class TabHolder extends BaseCustomObject {
    public name: string;
    private _tabs: Array<Tab>

    get tabs(): Array<Tab> {
        return this._tabs;
    }

    set tabs( value: Array<Tab> ) {
        this.getStateHolder().setPropertyAndHandleChanges( this, "_tabs", "tabs", value );
    }
}