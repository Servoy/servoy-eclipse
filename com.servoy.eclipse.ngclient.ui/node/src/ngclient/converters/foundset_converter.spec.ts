import { TestBed, inject, tick, fakeAsync } from '@angular/core/testing';
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
import { IFoundset } from '../../sablo/spectypes.service';
import { ViewportService} from '../services/viewport.service';

describe('FoundsetConverter', () => {
    
    const FILTER = "filter";
    const HANDLED = "handledID";
    const ID_KEY = "id";
    const VALUE_KEY = "value";
    const VALUES = "values";
    
    let converterService: ConverterService;
      
    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [FoundsetConverter,ConverterService,SabloService,SabloDeferHelper,SpecTypesService,LoggerFactory,WindowRefService,WebsocketService,ServicesService,SessionStorageService, ViewportService]
      });

      let sabloService: SabloService = TestBed.get( SabloService );
      sabloService.connect({}, {}, "");
      let sabloDeferHelper = TestBed.get( SabloDeferHelper );
      let viewportService = TestBed.get( ViewportService );
      let loggerFactory = TestBed.get( LoggerFactory );
      converterService = TestBed.get( ConverterService );
      converterService.registerCustomPropertyHandler( "foundset", new FoundsetConverter( converterService, sabloService, sabloDeferHelper, viewportService, loggerFactory) );
    });

    function createDefaultFoundset() {
        const json = {};
        json['foundsetId'] = 1;
        json['serverSize'] = 10;
        json['sortColumns'] = "";
        json['selectedRowIndexes'] = [0];
        json['multiselect'] = false;
        json['hasMoreRows'] = true;
        const viewport = {};
        viewport['startIndex'] = 0;
        viewport['size'] = 5;
        viewport['rows'] = [{'_svyRowId': "5.ALFKI;_0"},
                            {'_svyRowId': "5.ANATR;_1"},
                            {'_svyRowId': "5.AROUT;_3"},
                            {'_svyRowId': "5.BERGS;_4"},
                            {'_svyRowId': "5.BLAUS;_5"} ];
        json['viewport'] = viewport;
        return json;
    }

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
        //expect( fs.viewPort.size).toBe(5, "viewport size should be '5'");
        //expect( fs.viewPort.startIndex).toBe(0, "viewport startIndex should be '0'");
    });

});