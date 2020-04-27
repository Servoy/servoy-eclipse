import { TestBed} from '@angular/core/testing';
import { ConverterService, PropertyContext } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory } from '../../sablo/logger.service'
import { WindowRefService } from '../../sablo/util/windowref.service';
import { SpecTypesService } from '../../sablo/spectypes.service'
import { ServicesService } from '../../sablo/services.service'
import { FoundsetConverter } from './foundset_converter'
import { FoundsetLinkedConverter, FoundsetLinked } from './foundsetLinked_converter'
import { SabloDeferHelper} from '../../sablo/defer.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { SessionStorageService } from 'angular-web-storage';
import { ViewportService} from '../services/viewport.service';

describe('FoundsetLinked Converter', () => {
    let converterService: ConverterService;
    let loggerFactory: LoggerFactory;
    let sabloService: SabloService;
    let sabloDeferHelper: SabloDeferHelper;
    
    var componentModelGetter: PropertyContext;
	var serverValue: object;
	var realClientValue : FoundsetLinked;

    var changeNotified = false;
    
    beforeEach(() => {
        TestBed.configureTestingModule({
          providers: [FoundsetLinkedConverter, FoundsetConverter,ConverterService,SabloService,SabloDeferHelper,SpecTypesService,LoggerFactory,WindowRefService,WebsocketService,ServicesService,SessionStorageService, ViewportService]
        });
  
        sabloService = TestBed.get( SabloService );
        sabloService.connect({}, {}, "");
        sabloDeferHelper = TestBed.get( SabloDeferHelper );
        let viewportService = TestBed.get( ViewportService );
        loggerFactory = TestBed.get( LoggerFactory );
        converterService = TestBed.get( ConverterService );
        converterService.registerCustomPropertyHandler( "foundset", new FoundsetConverter( converterService, sabloService, sabloDeferHelper, viewportService, loggerFactory) );
        converterService.registerCustomPropertyHandler( "fsLinked", new FoundsetLinkedConverter( converterService, viewportService, loggerFactory) );
        changeNotified = false;

        var angularEquality = (first, second) => {
            return JSON.stringify(first) === JSON.stringify(second);//WAS angular.equals(first, second);
        };

        jasmine.addCustomEqualityTester(angularEquality);
      });

	function getAndClearNotified() {
		let tm = changeNotified;
		changeNotified = false;
		return tm;
	};

	describe("foundsetLinked_property with dumb values and simple values suite; pushToServer not set (so reject)", () => {
		beforeEach(() => {
			let myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" } ]
					}
			};
			let fs = converterService.convertFromServerToClient(myfoundset, 'foundset');
			componentModelGetter = (prop) => {
				return {
					myfoundset: fs
				}[prop];
			};

			serverValue = { forFoundset: "myfoundset" };

			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
			realClientValue.state.setChangeNotifier(() => { changeNotified = true });
			serverValue = { forFoundset: "myfoundset", sv: ":) --- static string ***" };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', realClientValue, componentModelGetter);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ ':) --- static string ***', ':) --- static string ***' ]);
		});


		it("Should not send value updates for when pushToServer is not specified", () => {
			// *** initial size no viewport
			realClientValue[1] = "I am changed but shouldn't be sent"; 

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
		});

	});

	describe("foundsetLinked_property with dumb values and simple values suite; pushToServer set to shallow", () => {
		beforeEach(() => {
			let myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" } ]
					}
			};
			let fs = converterService.convertFromServerToClient(myfoundset, 'foundset');
			componentModelGetter = (prop) => {
				return {
					myfoundset: fs
				}[prop];
			};

			serverValue = { forFoundset: "myfoundset", w: false };

			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
			realClientValue.state.setChangeNotifier(() => { changeNotified = true });

			serverValue = { sv: ":) --- static string ***" };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', realClientValue, componentModelGetter);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ ':) --- static string ***', ':) --- static string ***' ]);

		});

		it("Should send value updates for when pushToServer is false", () => {
            serverValue = { forFoundset: "myfoundset", w: false };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
			realClientValue.state.setChangeNotifier(() => { changeNotified = true });

			// *** initial size no viewport
			realClientValue.dataChanged(0, "I am really changed and I should be sent");
			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue.state.isChanged()).toEqual(true);
			expect(converterService.convertFromClientToServer(realClientValue, 'fsLinked', realClientValue)).toEqual(
					[ { propertyChange: 'I am really changed and I should be sent' } ]
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
		});

	});


	describe("foundsetLinked_property with dumb values and foundset linked values suite; pushToServer not set (so reject)", () => {
		beforeEach(() => {
			let myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" }, { "_svyRowId" : "bl bl" }, { "_svyRowId" : "ha ha" }, { "_svyRowId" : "b b" }, { "_svyRowId" : "h h" } ]
					}
			};
			let fs = converterService.convertFromServerToClient(myfoundset, 'foundset');
			componentModelGetter = (prop) => {
				return {
					myfoundset: fs
				}[prop];
			};

			serverValue = { forFoundset: "myfoundset" };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
			realClientValue.state.setChangeNotifier(() => { changeNotified = true });


			serverValue = {
					"forFoundset": "myfoundset",
					"vp": [ 10643, 10702, 10835, 10952, 11011, 11081 ]
			};

			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', realClientValue, componentModelGetter);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ 10643, 10702, 10835, 10952, 11011, 11081 ]);
		});


		it("Should not send value updates for when pushToServer is not specified", () => {
			// *** initial size no viewport
			realClientValue[2] = 100001010; 
			realClientValue.dataChanged(2, 100001010);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
		});

	});

	describe("foundsetLinked_property with dumb values and foundset linked values suite; pushToServer set to shallow", () => {
		beforeEach(() => {
			let myfoundset = {
					"serverSize": 0,
					"selectedRowIndexes": [],
					"multiSelect": false,
					"viewPort": 
					{
						"startIndex": 0,
						"size": 2,
						"rows": [ { "_svyRowId" : "bla bla" }, { "_svyRowId" : "har har" }, { "_svyRowId" : "bl bl" }, { "_svyRowId" : "ha ha" }, { "_svyRowId" : "b b" }, { "_svyRowId" : "h h" } ]
					}
			};
			let fs = converterService.convertFromServerToClient(myfoundset, 'foundset');
			componentModelGetter = (prop) => {
				return {
					myfoundset: fs
				}[prop];
			};

			serverValue = { forFoundset: "myfoundset", w: false };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
			realClientValue.state.setChangeNotifier(() => { changeNotified = true });

			serverValue = {
					"forFoundset": "myfoundset",
					"vp": [ 10643, 10702, 10835, 10952, 11011, 11081 ]
			};
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', realClientValue, componentModelGetter);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
			expect(realClientValue).toEqual([ 10643, 10702, 10835, 10952, 11011, 11081 ]);

		});

		it("Should not send value updates for when pushToServer is not specified", () => {
            serverValue = { forFoundset: "myfoundset", w:false, "vp": [ 10643, 10702, 10835, 10952, 11011, 11081 ] };
			realClientValue = converterService.convertFromServerToClient(serverValue, 'fsLinked', undefined, componentModelGetter);
            realClientValue.state.setChangeNotifier(() => { changeNotified = true });
            
			// *** initial size no viewport
           realClientValue.dataChanged(3, 1010101010);

			expect(getAndClearNotified()).toEqual(true);
			expect(realClientValue.state.isChanged()).toEqual(true);
			expect(converterService.convertFromClientToServer(realClientValue, 'fsLinked', realClientValue)).toEqual(
					[ { viewportDataChanged: { _svyRowId: 'ha ha', dp: null, value: 1010101010 } } ]
			);

			expect(getAndClearNotified()).toEqual(false);
			expect(realClientValue.state.isChanged()).toEqual(false);
		});

	});
})