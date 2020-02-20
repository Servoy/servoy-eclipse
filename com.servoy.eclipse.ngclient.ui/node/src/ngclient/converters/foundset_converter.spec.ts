import { TestBed, inject } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory } from '../../sablo/logger.service'
import { WindowRefService } from '../../sablo/util/windowref.service';
import { SpecTypesService } from '../../sablo/spectypes.service'
import { ServicesService } from '../../sablo/services.service'
import { FoundsetConverter, Foundset } from './foundset_converter'
import { SabloDeferHelper} from '../../sablo/defer.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { SessionStorageService } from 'angular-web-storage';
import { ViewportService} from '../services/viewport.service';
import { DateConverter } from './date_converter';

describe('FoundsetConverter', () => {
    
    const FILTER = "filter";
    const HANDLED = "handledID";
    const ID_KEY = "id";
    const VALUE_KEY = "value";
    const VALUES = "values";
    
    let converterService: ConverterService;
    let loggerFactory;
    let sabloService: SabloService;
    let sabloDeferHelper;
    let fs: Foundset;
    let changeNotified = false;
    let someDate;
    let someDateMs;
      
    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [FoundsetConverter,ConverterService,SabloService,SabloDeferHelper,SpecTypesService,LoggerFactory,WindowRefService,WebsocketService,ServicesService,SessionStorageService, ViewportService]
      });

      sabloService = TestBed.get( SabloService );
      sabloService.connect({}, {}, "");
      sabloDeferHelper = TestBed.get( SabloDeferHelper );
      let viewportService = TestBed.get( ViewportService );
      loggerFactory = TestBed.get( LoggerFactory );
      converterService = TestBed.get( ConverterService );
      converterService.registerCustomPropertyHandler( "foundset", new FoundsetConverter( converterService, sabloService, sabloDeferHelper, viewportService, loggerFactory) );
      converterService.registerCustomPropertyHandler( "Date", new DateConverter());

      changeNotified = false;
      someDate = new Date();
      someDateMs = someDate.getTime();
    });

    function createDefaultFoundset() {
        const json = {};
        json["foundsetId"] = 1;
        json["serverSize"] = 10;
        json["sortColumns"] = "";
        json["selectedRowIndexes"] = [0];
        json["multiSelect"] = false;
        json["hasMoreRows"] = true;
        const viewport = {};
        viewport["startIndex"] = 0;
        viewport["size"] = 5;
        viewport["rows"] = [{"_svyRowId": "5.ALFKI;_0"},
                            {"_svyRowId": "5.ANATR;_1"},
                            {"_svyRowId": "5.AROUT;_3"},
                            {"_svyRowId": "5.BERGS;_4"},
                            {"_svyRowId": "5.BLAUS;_5"} ];
        json["viewPort"] = viewport;
        return json;
    }

    function getAndClearNotified() {
		let tm = changeNotified;
		changeNotified = false;
		return tm;
      };

    it('should be created', inject([FoundsetConverter], (service: FoundsetConverter) => {
        expect(service).toBeTruthy();
    }));

    it( 'should convert from server to client', () => {
        let fs: Foundset = converterService.convertFromServerToClient(createDefaultFoundset() , "foundset" )
        expect( fs ).toBeDefined();
        expect( fs.foundsetId ).toBe(1, "foundsetId should be '1' ");
        expect( fs.serverSize ).toBe(10, "foundset serverSize should be '10");
        expect( fs.sortColumns ).toBe("", "foundset sort columns should be empty");
        expect( fs.multiSelect ).toBe(false, "foundset should not allow multiSelect");
        expect( fs.selectedRowIndexes.length).toBe(1, "foundset should have '1' record selected ");
        expect( fs.selectedRowIndexes[0]).toBe(0, "foundset should have first record selected");
        expect( fs.hasMoreRows).toBe(true, "foundset should have more rows");
        expect( fs.viewPort ).toBeDefined();
        expect( fs.viewPort.size).toBe(5, "viewport size should be '5'");
        expect( fs.viewPort.startIndex).toBe(0, "viewport startIndex should be '0'");
    });

    it('should not send change of int value to server when no pushToServer is specified for property', () => {
        let serverValue = {
                "serverSize": 0,
                "selectedRowIndexes": [],
                "multiSelect": false,
                "viewPort": {
                    "startIndex": 0,
                    "size": 0,
                    "rows": [{
                         "d": someDateMs, "i": 1234, "_svyRowId": "5.10643;_0"
                     }]
                }
        };

        fs = converterService.convertFromServerToClient(serverValue,'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });
        expect(getAndClearNotified()).toEqual(false);

        // so no "w": false in server received value...
        let tmp = fs.viewPort.rows[0]["i"];
        fs.viewPort.rows[0]["i"] = 4321234;
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);
        fs.viewPort.rows[0]["i"] = tmp;
    });

    it("Should get viewport size then request viewport and change selection", ()  => {
        // *** initial size no viewport
        let updateValue = {
                "serverSize": 6,
                "w": false,
                "selectedRowIndexes":[0],
                "multiSelect": false,
                "viewPort": 
                {
                    "startIndex": 0,
                    "size": 0,
                    "rows": []
                }
        };
        fs = converterService.convertFromServerToClient(updateValue,'foundset', fs);
        fs.state.setChangeNotifier(function () { changeNotified = true });
        let copy = Object.assign({}, updateValue);
        delete copy["w"]; // this one goes to internal state
        // TODO expect(fs).toEqual(copy);

        // *** request and receive new viewport (all records in this case)
        fs.loadRecordsAsync(0,6);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        let updates = converterService.convertFromClientToServer(fs, 'foundset', fs);
        expect(updates[0].newViewPort).toEqual({"startIndex":0,"size":6});
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);

        fs = converterService.convertFromServerToClient({
            "upd_viewPort": 
            {
                "startIndex": 0,
                "size": 6,
                "svy_types": {"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"4":{"d":"Date"},"5":{"d":"Date"}}},
                "rows": 
                    [
                     { "d": someDateMs, "i": 1234, "_svyRowId": "5.10643;_0" },
                     { "d": someDateMs, "i": 1234, "_svyRowId": "5.10692;_1" },
                     { "d": someDateMs, "i": 1234, "_svyRowId": "5.10702;_2" },
                     { "d": null, "i": 1234, "_svyRowId": "5.10835;_3" },
                     { "d": someDateMs, "i": 1234, "_svyRowId": "5.10952;_4" },
                     { "d": someDateMs, "i": 1234, "_svyRowId": "5.11011;_5" }
                     ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 6;
        expectedfs.selectedRowIndexes = [0];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 6, rows: [] };
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"});
        expectedfs.viewPort.rows.push({"d": null, "i": 1234, "_svyRowId": "5.10835;_3"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"});
        expect(fs).toEqual(expectedfs);

        // *** Selection change from Client
        fs.requestSelectionUpdate([1]); // WAS fs.selectedRowIndexes[0] = 1;
        expect(fs.state.isChanged()).toEqual(true);
        updates = converterService.convertFromClientToServer(fs, 'foundset', fs)
        expect(updates[0].newClientSelectionRequest).toEqual([1], "The selection change notification sent");
        expect(fs.state.isChanged()).toEqual(false);

        fs = converterService.convertFromServerToClient({"upd_selectedRowIndexes":[2]},'foundset', fs);
		expect(fs.selectedRowIndexes.length).toBe(1);
        expect(fs.selectedRowIndexes[0]).toBe(2, "should get selection update from server");
    });

    it("Should insert 2 before selection (server); this is a special case where foundset automatically expands viewport if viewport was showing whole foundset (an optimisation for scroll views)", () => {
        let serverValue = {
            "serverSize": 0,
            "selectedRowIndexes": [2],
            "multiSelect": false,
            "viewPort": {
                "startIndex": 0,
                "size": 6,
                "rows": [{"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"},
                        {"d": null, "i": 1234, "_svyRowId": "5.10835;_3"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"}]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue,'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 8,
            "upd_selectedRowIndexes": [4],
            "upd_viewPort": 
            {
                "startIndex": 0,
                "size": 8,
                "svy_types": {"upd_rows":{"2":{"rows":{"0":{"d":"Date"}}},"3":{"rows":{"0":{"d":"Date"}}}}},
                "upd_rows": 
                [
                    {
                        "rows": [{"d": null, "i": 1234, "_svyRowId": "5.11078;_1"}],
                        "startIndex": 1,
                        "endIndex": 1,
                        "type": 1
                    },
                    {
                        "rows": [{"d": null, "i": 1234, "_svyRowId": "5.11078;_1"}],
                        "startIndex": 1,
                        "endIndex": 1,
                        "type": 0
                    },
                    {
                        "rows": [{"d": someDateMs, "i": 1234, "_svyRowId": "5.11079;_2"}],
                        "startIndex": 2,
                        "endIndex": 2,
                        "type": 1
                    },
                    {
                        "rows": [{"d": someDateMs, "i": 1234, "_svyRowId": "5.11079;_2"}],
                        "startIndex": 2,
                        "endIndex": 2,
                        "type": 0
                    }
                ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 8;
        expectedfs.selectedRowIndexes = [4];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 8, rows: [] };
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"});
        expectedfs.viewPort.rows.push({"d": null, "i": 1234, "_svyRowId": "5.11078;_1"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11079;_2"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"});
        expectedfs.viewPort.rows.push({"d": null, "i": 1234, "_svyRowId": "5.10835;_3"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"});    
        expect(fs).toEqual(expectedfs);
        expect(fs.selectedRowIndexes[0]).toEqual(4);
    });

    it("Should remove the inserted 2 (server) and one more (1-3)", () => {
        
        let serverValue = {
            "serverSize": 0,
            "selectedRowIndexes": [4],
            "multiSelect": false,
            "viewPort": {
                "startIndex": 0,
                "size": 8,
                "rows": [{"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"},
                        {"d": null, "i": 1234, "_svyRowId": "5.11078;_1"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.11079;_2"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10692;_1"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"},
                        {"d": null, "i": 1234, "_svyRowId": "5.10835;_3"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"},
                        {"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"}]
            }
        };
        fs = converterService.convertFromServerToClient(serverValue,'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 5,
            "upd_selectedRowIndexes": 
            [
                1
            ],

            "upd_viewPort": 
            {
                "startIndex": 0,
                "size": 5,
                "upd_rows": 
                [
                    { "startIndex": 1, "endIndex": 1, "type": 2 },
                    { "startIndex": 1, "endIndex": 1, "type": 2 },
                    { "startIndex": 1, "endIndex": 1, "type": 2 }
                ]
            }
        },'foundset', fs);
        
        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 5;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 5, rows: [] };
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10643;_0"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10702;_2"});
        expectedfs.viewPort.rows.push({"d": null, "i": 1234, "_svyRowId": "5.10835;_3"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.10952;_4"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11011;_5"});   
        expect(fs).toEqual(expectedfs);
    });

    it("Foundset changed completely (relation & parent record changed for example on server - to something that is larger then we want to cache; so we will request smaller viewport)", () => {
        fs = converterService.convertFromServerToClient(createDefaultFoundset(), 'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 12,
            "upd_selectedRowIndexes": [ 0 ],
            "upd_viewPort": { "startIndex": 0, "size": 0, "rows": [] }
        },'foundset', fs);
        
        expect(fs.serverSize).toEqual(12);
        expect(fs.selectedRowIndexes[0]).toEqual(0);
        expect(fs.viewPort.size).toEqual(0);
        expect(fs.viewPort.rows.length).toEqual(0);
        
        expect(getAndClearNotified()).toEqual(false);
        // *** request and receive new viewport (all except 3 records at the end)
        fs.loadRecordsAsync(0,9);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        let updates = converterService.convertFromClientToServer(fs, 'foundset', fs)
        expect(updates[0].newViewPort).toEqual({"startIndex":0,"size":9});
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);
        
        // *** viewport comes from server
        const rows = [
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_6" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11104;_7" },
            { "d": someDateMs, "i": 1234, "_svyRowId": "5.11105;_8" }
        ];
        fs = converterService.convertFromServerToClient({
            "upd_viewPort": 
            {
                "startIndex": 0,
                "size": 9,
                "svy_types": {"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"3":{"d":"Date"},"4":{"d":"Date"},"5":{"d":"Date"},"6":{"d":"Date"},"7":{"d":"Date"},"8":{"d":"Date"}}},
                "rows": rows
            }
        },'foundset', fs);
        
        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state); //we don't care too much about the state on the expectedfs, it has to be equal to the original one
        expectedfs.serverSize = 12;
        expectedfs.selectedRowIndexes = [0];
        expectedfs.multiSelect = false;
        expectedfs.foundsetId = 1;
        expectedfs.sortColumns = '';
        expectedfs.hasMoreRows = true;
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows };
        expect(fs).toEqual(expectedfs);
    });

    it("Should insert 2 at index 1 (now viewport stays the same as bounds but 2 get inserted and 2 from bottom get removed)", () => {
        let serverValue = {
            "serverSize": 12,
            "selectedRowIndexes": [ 4 ],
            "multiSelect": false,
            "viewPort": {
                "startIndex": 0,
                "size": 9,
                "rows": [{ "d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_6" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11104;_7" },
                        { "d": someDateMs, "i": 1234, "_svyRowId": "5.11105;_8" }
                        ]
                    }
                };
        fs = converterService.convertFromServerToClient(serverValue, 'foundset'); 
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 14,
            "upd_selectedRowIndexes": [ 2 ],
            "upd_viewPort": 
            {
                "svy_types": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}},"1":{"rows":{"0":{"d":"Date"}}},"2":{"rows":{"0":{"d":"Date"}}},"3":{"rows":{"0":{"d":"Date"}}},"4":null}},
                "upd_rows": 
                [
                    {
                        "rows":  [ { "d": someDate, "i": 1234, "_svyRowId": "5.1112;_1" } ],
                        "startIndex": 0,
                        "endIndex": 0,
                        "type": 1
                    },

                    {
                        "rows": [ { "d": someDate, "i": 1234, "_svyRowId": "5.11112;_1" } ],
                        "startIndex": 0,
                        "endIndex": 0,
                        "type": 0
                    },

                    {
                        "rows": [ { "d": someDate, "i": 1234, "_svyRowId": "5.11113;_2" } ],
                        "startIndex": 1,
                        "endIndex": 1,
                        "type": 1
                    },

                    {
                        "rows":  [ { "d": someDate, "i": 1234, "_svyRowId": "5.11113;_2" }],
                        "startIndex": 1,
                        "endIndex": 1,
                        "type": 0
                    }, 
                    {
                        "startIndex": 9,
                        "endIndex": 10,
                        "type": 2
                    }
                ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 14;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: [] };
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"});
        expectedfs.viewPort.rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5"});
        expectedfs.viewPort.rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_6"});
        expect(fs).toEqual(expectedfs);
    });

    it("Should insert at last position (but still part of foundset)", () => {
        let rows = [{ "d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_6" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11104;_7" },
                    { "d": someDateMs, "i": 1234, "_svyRowId": "5.11105;_8" }
                ];
        let serverValue = {
            "serverSize": 14,
            "selectedRowIndexes": [ 2 ],
            "multiSelect": false,
            "viewPort": { "startIndex": 0, "size": 9, "rows": rows }
        };
        fs = converterService.convertFromServerToClient(serverValue, 'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 15,
            "upd_viewPort": 
            {
                "svy_types": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"}}},"1":null,"2":{"rows":{"0":{"d":"Date"}}}}},
                "upd_rows": 
                [
                    {
                        "rows": [ { "d": someDateMs, "i": 1234, "_svyRowId": "5.11115;_29" } ],
                        "startIndex": 8,
                        "endIndex": 8,
                        "type": 1
                    },

                    {
                        "startIndex": 9,
                        "endIndex": 9,
                        "type": 2
                    },

                    {
                        "rows": [ { "d": someDateMs, "i": 1234, "_svyRowId": "5.11115;_29" } ],
                        "startIndex": 8,
                        "endIndex": 8,
                        "type": 0
                    }
                ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 15;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        rows[8] = {"d": someDateMs, "i": 1234, "_svyRowId": "5.11115;_29"};
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows };
        expect(fs).toEqual(expectedfs);
    });

    it("Should delete last position of viewport (new record should be received in its place)", () => {
        let rows = [{"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"},
                    {"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5"},
                    { "d": someDate, "i": 1234, "_svyRowId": "5.11115;_29" }
                ];
        let serverValue = {
            "serverSize": 15,
            "selectedRowIndexes": [ 2 ],
            "multiSelect": false,
            "viewPort": { "startIndex": 0, "size": 9, "rows": rows }
        };
        fs = converterService.convertFromServerToClient(serverValue, 'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 14,
            "upd_viewPort": 
            {
                "svy_types": {"upd_rows":{"0":null,"1":{"rows":{"0":{"d":"Date"}}}}},
                "upd_rows": 
                [
                    {
                        "startIndex": 8,
                        "endIndex": 8,
                        "type": 2
                    },
                    
                    {
                        "rows": [ { "d": someDate, "i": 1234, "_svyRowId": "5.11106;_29" } ],

                        "startIndex": 8,
                        "endIndex": 8,
                        "type": 1
                    }
                ]
            }
        },'foundset', fs);
       
        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 14;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        rows[8] = { "d": someDate, "i": 1234, "_svyRowId": "5.11106;_29" };
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows};
        expect(fs).toEqual(expectedfs);
    });

    it("Should delete first position (new record should be received in its place)", function() {
        let rows = [{"d": someDate, "i": 1234, "_svyRowId": "5.11112;_1"},
                    {"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5"},
                    {"d": someDate, "i": 1234, "_svyRowId": "5.11106;_29"}
                ];
        let serverValue = {
            "serverSize": 15,
            "selectedRowIndexes": [ 2 ],
            "multiSelect": false,
            "viewPort": { "startIndex": 0, "size": 9, "rows": rows }
        };
        fs = converterService.convertFromServerToClient(serverValue, 'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs = converterService.convertFromServerToClient({
            "upd_serverSize": 13,
            "upd_selectedRowIndexes": [ 1 ],
            "upd_viewPort": 
            {
                "svy_types": {"upd_rows":{"0":null,"1":{"rows":{"0":{"d":"Date"}}}}},
                "upd_rows": 
                [
                    {
                        "startIndex": 0,
                        "endIndex": 0,
                        "type": 2
                    },
                    
                    {
                        "rows": 
                        [
                            {
                                "d": someDateMs, "i": 1234, "_svyRowId": "5.11107;_29"
                            }
                        ],
                        "startIndex": 8,
                        "endIndex": 8,
                        "type": 1
                    }
                ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 13;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        rows.shift();
        rows.push({"d": someDate, "i": 1234, "_svyRowId": "5.11107;_29"});
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows};
        expect(fs).toEqual(expectedfs);
    });

    it("Should scroll down to bottom of foundset - viewport needs to be expanded )", function() {
        let rows = [{"d": someDate, "i": 1234, "_svyRowId": "5.11113;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.10350;_0"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11110;_1"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11111;_2"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11108;_3"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11109;_4"},
                    {"d": someDateMs, "i": 1234, "_svyRowId": "5.11106;_5"},
                    {"d": someDate, "i": 1234, "_svyRowId": "5.11106;_29"}
                ];
        let serverValue = {
            "serverSize": 13,
            "selectedRowIndexes": [ 1 ],
            "multiSelect": false,
            "viewPort": { "startIndex": 0, "size": 9, "rows": rows }
        };
        fs = converterService.convertFromServerToClient(serverValue, 'foundset');
        fs.state.setChangeNotifier(() => { changeNotified = true });

        fs.loadExtraRecordsAsync(4);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        let updates = converterService.convertFromClientToServer(fs, 'foundset', fs)
        expect(updates[0].loadExtraRecords).toEqual(4);
        
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);
        
        fs = converterService.convertFromServerToClient({
            "upd_viewPort": 
            {
                "startIndex": 0,
                "size": 13,
                "svy_types": {"upd_rows":{"0":{"rows":{"0":{"d":"Date"},"1":{"d":"Date"},"2":{"d":"Date"},"3":{"d":"Date"}}}}},
                "upd_rows": 
                [
                    {
                        "rows": [  { "d": someDateMs, "i": 1234, "_svyRowId": "5.10610;_9" },
                            { "d": someDateMs, "i": 1234, "_svyRowId": "5.10631;_10"},
                            { "d": someDateMs, "i": 1234, "_svyRowId": "5.10787;_11"},
                            { "d": someDateMs, "i": 1234, "_svyRowId": "5.10832;_12"}
                        ],

                        "startIndex": 9,
                        "endIndex": 12,
                        "type": 1
                    }
                ]
            }
        },'foundset', fs);

        let expectedfs = new Foundset(sabloService, sabloDeferHelper, loggerFactory, converterService, fs.state);
        expectedfs.serverSize = 13;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.10610;_9"});
        rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.10631;_10"});
        rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.10787;_11"});
        rows.push({"d": someDateMs, "i": 1234, "_svyRowId": "5.10832;_12"});
        expectedfs.viewPort = { startIndex: 0, size: 13, rows: rows};
        expect(fs).toEqual(expectedfs);

        //Should send change of date value to server
        /*let newDate : Date = fs.viewPort.rows[12]['d'] = new Date(new Date().getTime() + 1);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, 'foundset', fs)).toEqual(
                [ { viewportDataChanged: { _svyRowId: '5.10832;_12', dp: 'd', value: newDate.getTime() } } ]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);
        
        //Should send change of int value to server
        fs.viewPort.rows[0]['i']= 4321;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, 'foundset', fs)).toEqual(
                [ { viewportDataChanged: { _svyRowId: '5.11113;_2', dp: 'i', value: 4321 } } ]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);
        
        //Should send sort to server
        fs.sort([{"name":'i', "direction":"asc"}, {"name":'d', "direction":"desc"}]);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.state.isChanged()).toEqual(true);
        var message = converterService.convertFromClientToServer(fs, 'foundset', fs)
        delete message[0].id
        expect(message).toEqual(
                [ { sort: [ { name: 'i', direction: 'asc' }, { name: 'd', direction: 'desc' } ] } ]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.state.isChanged()).toEqual(false);*/
    });
});