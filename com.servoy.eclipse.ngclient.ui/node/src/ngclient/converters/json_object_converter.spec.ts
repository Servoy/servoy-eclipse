import { TestBed, inject } from '@angular/core/testing';


import { ConverterService } from '../../sablo/converter.service';
import { LoggerService } from '../../sablo/logger.service'

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
    const TYPES_KEY = 'svy_types';

    let converterService: ConverterService
    let specTypesService: SpecTypesService
    beforeEach(() => {
        TestBed.configureTestingModule( {
            providers: [ConverterService, SpecTypesService, LoggerService]
        } );
        specTypesService = TestBed.get( SpecTypesService );
        converterService = TestBed.get( ConverterService );
        converterService.registerCustomPropertyHandler( "JSON_obj", new JSONObjectConverter( converterService, specTypesService ) );
        specTypesService.registerType( "Tab", Tab, ["name", "myvalue"] );
        specTypesService.registerType( "TabHolder", TabHolder, ["id", "tab", "tab2", "tab3"] );
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
        const tab = specTypesService.createType( "Tab" );
        expect( tab ).toBeDefined();
        expect( tab instanceof Tab ).toBeTruthy();
    } );

    it( 'object should be created changed from server side', () => {
        let tab: Tab = createTabObjectFromServer();
        expect( tab ).toBeDefined();
        expect( tab instanceof Tab ).toBeTruthy( "tab should be instance of Tab" );
        expect( tab.name ).toBe( "test", "name should be test" );
        expect( tab.myvalue ).toBe( "test", "myvalue should be test" );
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );

        const update = {};
        update[KEY] = "myvalue";
        update[VALUE] = "test2";
        const json = {};
        json[UPDATES] = [update];
        json[CONTENT_VERSION] = 1;
        tab = converterService.convertFromServerToClient( json, "JSON_obj", tab );
        expect( tab.myvalue ).toBe( "test2", "myvalue should be test2" );
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
    } );

    it( 'object created and changed from client side', () => {
        let tab: Tab = createTabObjectFromServer();
        tab.name = "test2";
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes because name is not monitored" );
        tab.myvalue = "test";
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes because of same assignment" );
        tab.myvalue = "test2";
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 1, "should have changes because of  assignment" );
        const changes = converterService.convertFromClientToServer( tab, "JSON_obj" );
        expect( changes ).toBeDefined( "change object should be generated" );
        expect( changes[CONTENT_VERSION] ).toBe( 1 );
        expect( changes[UPDATES] ).toBeDefined( "change object  shoulld have updates" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE] ).toBe( "test2" );
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
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
        expect( tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );

    } );

    it( 'nested object created in scripting', () => {
        const tabHolder = new TabHolder();
        tabHolder.id = "test";
        tabHolder.tab2 = new Tab();
        tabHolder.tab2.name = "test";
        tabHolder.tab2.myvalue = "test";
        expect( tabHolder.getStateHolder().allChanged ).toBe( true, "should have changes as it is new" );
        let changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[VALUE] ).toBeDefined( "change object should have a value" );
        expect( changes[VALUE].id ).toBe( "test" );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBeUndefined( "it has been sent to server but it should still not have a content version until server sends that back" ); // it used to be NaN at some point
        expect( changes[VALUE].tab2[VALUE] ).toBeDefined();
        expect( changes[VALUE].tab2[VALUE].name ).toBe( "test" );
        expect( changes[VALUE].tab2[VALUE].myvalue ).toBe( "test" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.tab2.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );

        tabHolder.tab2.name = "test2";
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        tabHolder.tab2.myvalue = "test2";
        expect( tabHolder.tab2.getStateHolder().getChangedKeys().length ).toBe( 1, "should have changes" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 1, "should have changes" );

        // although it only has changes, if it it sent back to server again it should still send whole value as it didn't yet get the content-version from server
        changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[VALUE] ).toBeDefined( "change object should have a value" );
        expect( changes[VALUE].id ).toBe( "test" );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBeUndefined( "it has been sent to server but it should still not have a content version until server sends that back" ); // it used to be NaN at some point
        expect( changes[VALUE].tab2[VALUE] ).toBeDefined();
        expect( changes[VALUE].tab2[VALUE].name ).toBe( "test2" );
        expect( changes[VALUE].tab2[VALUE].myvalue ).toBe( "test2" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.tab2.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );

        // ok now simulate an initialize + content version set for the new value comming from the server
        let msgFromServer = {};
        msgFromServer[CONTENT_VERSION] = 1;
        msgFromServer[INITIALIZE] = true;
        msgFromServer[UPDATES] = [];
        msgFromServer[UPDATES].push( {} );
        msgFromServer[UPDATES][0][KEY] = 'tab2';
        msgFromServer[UPDATES][0][VALUE] = {};
        msgFromServer[UPDATES][0][VALUE][CONTENT_VERSION] = 2;
        msgFromServer[UPDATES][0][VALUE][INITIALIZE] = true;
        msgFromServer[TYPES_KEY] = { 0: {} };
        msgFromServer[TYPES_KEY]['0'][VALUE] = "JSON_obj";

        converterService.convertFromServerToClient( msgFromServer, "JSON_obj", tabHolder, null, null );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBe( 1, "it got the new version from server" );
        expect( tabHolder.tab2.getStateHolder()[CONTENT_VERSION] ).toBe( 2, "it got the new version from server" );

        // now add another child and see that sending it and getting it back works ok (it will go through another branch if the new obj. does not have child objs)
        tabHolder.tab3 = new Tab();
        tabHolder.tab3.name = "test";
        tabHolder.tab3.myvalue = "test";

        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "should not be completely changed" );
        changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[VALUE] ).toBeUndefined( "change object should have a value" );
        expect( changes[UPDATES] ).toBeDefined( "should have updates" );
        expect( changes[UPDATES][0][KEY] ).toBe( 'tab3' );
        expect( changes[UPDATES][0][VALUE] ).toBeDefined();
        expect( changes[UPDATES][0][VALUE][VALUE] ).toBeDefined();
        expect( changes[UPDATES][0][VALUE][VALUE].name ).toBe( "test" );
        expect( changes[UPDATES][0][VALUE][VALUE].myvalue ).toBe( "test" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.tab3.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );

        // ok now simulate an initialize + content version set for the new value comming from the server
        msgFromServer = {};
        msgFromServer[CONTENT_VERSION] = 1;
        msgFromServer[UPDATES] = [];
        msgFromServer[UPDATES].push( {} );
        msgFromServer[UPDATES][0][KEY] = 'tab3';
        msgFromServer[UPDATES][0][VALUE] = {};
        msgFromServer[UPDATES][0][VALUE][CONTENT_VERSION] = 3;
        msgFromServer[UPDATES][0][VALUE][INITIALIZE] = true;
        msgFromServer[TYPES_KEY] = { 0: {} };
        msgFromServer[TYPES_KEY]['0'][VALUE] = "JSON_obj";

        converterService.convertFromServerToClient( msgFromServer, "JSON_obj", tabHolder, null, null );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBe( 1, "it got the new version from server" );
        expect( tabHolder.tab3.getStateHolder()[CONTENT_VERSION] ).toBe( 3, "it got the new version from server" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "it should have no more outgoing changes" );
    } );

    it( 'nested objects - if somehow marked as allchanged == true should send full values in-depth', () => {
        const tabHolder = new TabHolder();
        tabHolder.id = "test";
        tabHolder.tab = new Tab();
        tabHolder.tab.name = "test";
        tabHolder.tab.myvalue = "test";
        expect( tabHolder.getStateHolder().allChanged ).toBe( true, "should have changes as it is new" );
        let changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "it should have no more outgoing changes" );

        // simulate init from server so that they have content versions (so that lack of content versions will not determine them to send themselves fully)
        const msgFromServer = {};
        msgFromServer[CONTENT_VERSION] = 1;
        msgFromServer[INITIALIZE] = true;
        msgFromServer[UPDATES] = [];
        msgFromServer[UPDATES].push( {} );
        msgFromServer[UPDATES][0][KEY] = 'tab';
        msgFromServer[UPDATES][0][VALUE] = {};
        msgFromServer[UPDATES][0][VALUE][CONTENT_VERSION] = 3;
        msgFromServer[UPDATES][0][VALUE][INITIALIZE] = true;
        msgFromServer[TYPES_KEY] = { 0: {} };
        msgFromServer[TYPES_KEY]['0'][VALUE] = "JSON_obj";

        converterService.convertFromServerToClient( msgFromServer, "JSON_obj", tabHolder, null, null );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBe( 1, "it got the new version from server" );
        expect( tabHolder.tab.getStateHolder()[CONTENT_VERSION] ).toBe( 3, "it got the new version from server" );

        // now make only one prop. change in nested tab
        tabHolder.tab.myvalue = "test1";
        expect( tabHolder.tab.getStateHolder().getChangedKeys().length ).toBe( 1, "we just changed myvalue" );
        expect( tabHolder.tab.getStateHolder().allChanged ).toBe( false, "we just changed myvalue, not the whole thing" );

        // simulate that it should be sent fully
        tabHolder.getStateHolder().markAllChanged( false ); // could be set at runtime for example by a parent custom object or custom array type when that one wants to be sent fully
        changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )

        // check that both tabHolder and tab are fully sent (so for tab we don't send just the change to myvalue)
        expect( changes[VALUE] ).toBeDefined( "it should send full value to server" );
        expect( changes[VALUE].id ).toBe( "test" );
        expect( changes[VALUE].tab[VALUE] ).toBeDefined();
        expect( changes[VALUE].tab[VALUE].name ).toBe( "test" );
        expect( changes[VALUE].tab[VALUE].myvalue ).toBe( "test1" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.tab.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "should not have changes" );
        expect( tabHolder.tab.getStateHolder().allChanged ).toBe( false, "should not have changes" );
    } );

    it( 'nested object from server', () => {
        const data = { id: "test", tab2: createTabJSON() };
        const json = {};
        json[VALUE] = data;
        json[REAL_TYPE] = "TabHolder";
        json[CONTENT_VERSION] = 1;
        json[ConverterService.TYPES_KEY] = { "tab": "JSON_obj", "tab2": "JSON_obj" };
        const tabHolder: TabHolder = converterService.convertFromServerToClient( json, "JSON_obj" );

        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.tab2.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "should not have changes" );
        expect( tabHolder.tab2.getStateHolder().allChanged ).toBe( false, "should not have changes" );

        tabHolder.tab2.name = "test2";
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "should not have changes" );
        tabHolder.tab2.myvalue = "test2";
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 1, "should have changes" );

        const changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )
        expect( changes[UPDATES] ).toBeDefined( "change object  should have an update value" );
        expect( changes[UPDATES].length ).toBe( 1, "should have 1 update" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tab2" );
        expect( changes[UPDATES][0][VALUE][UPDATES] ).toBeDefined( "change object  should have an update value" );
        expect( changes[UPDATES][0][VALUE][UPDATES].length ).toBe( 1, "should have 1 update" );

        expect( changes[UPDATES][0][VALUE][UPDATES][0][KEY] ).toBe( "myvalue" );
        expect( changes[UPDATES][0][VALUE][UPDATES][0][VALUE] ).toBe( "test2" );
    } );

    it( 'nested object has 2 props of the same type, both with values, then the sub-values change the property to which they are assigned', () => {
        const tabHolder = new TabHolder();
        tabHolder.id = "test";
        tabHolder.tab3 = new Tab();
        tabHolder.tab3.name = "tab3Name";
        tabHolder.tab3.myvalue = "tab3MyValue";
        tabHolder.tab2 = new Tab();
        tabHolder.tab2.name = "tab2Name";
        tabHolder.tab2.myvalue = "tab2MyValue";
        expect( tabHolder.getStateHolder().allChanged ).toBe( true, "should have changes as it is new" );
        let changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" ); // this will clear changes

        // simulate init from server so that they have content versions (so that lack of content versions will not determine them to send themselves fully)
        const msgFromServer = {};
        msgFromServer[CONTENT_VERSION] = 1;
        msgFromServer[INITIALIZE] = true;
        msgFromServer[UPDATES] = [];
        msgFromServer[UPDATES].push( {} );
        msgFromServer[UPDATES][0][KEY] = 'tab2';
        msgFromServer[UPDATES][0][VALUE] = {};
        msgFromServer[UPDATES][0][VALUE][CONTENT_VERSION] = 3;
        msgFromServer[UPDATES][0][VALUE][INITIALIZE] = true;
        msgFromServer[UPDATES].push( {} );
        msgFromServer[UPDATES][1][KEY] = 'tab3';
        msgFromServer[UPDATES][1][VALUE] = {};
        msgFromServer[UPDATES][1][VALUE][CONTENT_VERSION] = 4;
        msgFromServer[UPDATES][1][VALUE][INITIALIZE] = true;
        msgFromServer[TYPES_KEY] = { 0: {}, 1: {} };
        msgFromServer[TYPES_KEY]['0'][VALUE] = "JSON_obj";
        msgFromServer[TYPES_KEY]['1'][VALUE] = "JSON_obj";

        converterService.convertFromServerToClient( msgFromServer, "JSON_obj", tabHolder, null, null );
        expect( tabHolder.getStateHolder()[CONTENT_VERSION] ).toBe( 1, "it got the new version from server" );
        expect( tabHolder.tab2.getStateHolder()[CONTENT_VERSION] ).toBe( 3, "it got the new version from server" );
        expect( tabHolder.tab3.getStateHolder()[CONTENT_VERSION] ).toBe( 4, "it got the new version from server" );

        // ok now everything is 'stable': has versions and no changes
        expect( tabHolder.tab2.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.tab3.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.tab2.getStateHolder().allChanged ).toBe( false, "no change expected" );
        expect( tabHolder.tab3.getStateHolder().allChanged ).toBe( false, "no change expected" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "no change expected" );

        // simulate that tab is tab3 gets the value of tab2 (so tab3's value is no longer used)
        let obsoleteTab3 = tabHolder.tab3;
        tabHolder.tab3 = tabHolder.tab2;
        tabHolder.tab2 = null;
        changes = converterService.convertFromClientToServer( tabHolder, "JSON_obj" )

        // check that both tabHolder and tab are fully sent (so for tab we don't send just the change to myvalue)
        expect( changes[UPDATES] ).toBeDefined( "change object  should have an update value" );
        expect( changes[UPDATES].length ).toBe( 2, "should have 2 updates" );
        expect( changes[UPDATES][0][KEY] ).toBe( "tab3" );
        expect( changes[UPDATES][0][VALUE][VALUE] ).toBeDefined( "should send full value as it changed by ref" );
        expect( changes[UPDATES][0][VALUE][VALUE].name ).toBe( "tab2Name", "should have old tab2's name" );
        expect( changes[UPDATES][0][VALUE][VALUE].myvalue ).toBe( "tab2MyValue", "should have old tab2's myValue" );
        expect( changes[UPDATES][1][KEY] ).toBe( "tab2" );
        expect( changes[UPDATES][1][VALUE] ).toBeNull( "tab2 was set to null" );

        expect( tabHolder.tab3.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.tab3.getStateHolder().allChanged ).toBe( false, "no change expected" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "no change expected" );

        // changing something in old tab3'v value should not trigger any changes as that value is no longer used
        obsoleteTab3.myvalue = "aha";

        expect( tabHolder.tab3.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.tab3.getStateHolder().allChanged ).toBe( false, "no change expected" );
        expect( tabHolder.getStateHolder().getChangedKeys().length ).toBe( 0, "no change expected" );
        expect( tabHolder.getStateHolder().allChanged ).toBe( false, "no change expected" );
    } );
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

    id: string;
    tab: Tab;
    protected _tab2: Tab;
    protected _tab3: Tab;

    get tab2() { return this._tab2 }

    set tab2( tab: Tab ) {
        this.getStateHolder().setPropertyAndHandleChanges( this, "_tab2", "tab2", tab );
    }

    get tab3() { return this._tab3 }

    set tab3( tab: Tab ) {
        this.getStateHolder().setPropertyAndHandleChanges( this, "_tab3", "tab3", tab );
    }
}