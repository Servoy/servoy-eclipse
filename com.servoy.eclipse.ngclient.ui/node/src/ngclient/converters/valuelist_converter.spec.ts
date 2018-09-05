import { TestBed, inject, tick, fakeAsync } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory } from '../../sablo/logger.service'
import { WindowRefService } from '../../sablo/util/windowref.service';
import { SpecTypesService } from '../../sablo/spectypes.service'
import { ServicesService } from '../../sablo/services.service'
import { ValuelistConverter,Valuelist } from './valuelist_converter'
import { SabloDeferHelper} from '../../sablo/defer.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { SessionStorageService } from 'angular-web-storage';
import { IValuelist } from '../../sablo/spectypes.service'

describe('ValuelistConverter', () => {
    
  const FILTER = "filter";
  const HANDLED = "handledID";
  const ID_KEY = "id";
  const VALUE_KEY = "value";
  const VALUES = "values";
  
  let converterService: ConverterService;
    
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ValuelistConverter,ConverterService,SabloService,SabloDeferHelper,SpecTypesService,LoggerFactory,WindowRefService,WebsocketService,ServicesService,SessionStorageService]
    });
    
    let sabloService: SabloService = TestBed.get( SabloService );
    sabloService.callService = (serviceName:string, methodName:string, argsObject, async?:boolean) => {
        if (serviceName == 'formService' && methodName == 'getValuelistDisplayValue')
        {
            var promise = new Promise((resolve, reject) => {
                if (argsObject.realValue === 4) {
                    resolve("d")
                }
                else {
                    reject("No display value found for "+argsObject.realValue);
                }
            });
            return promise;
        }
    }
    sabloService.connect({}, {}, "");
    let sabloDeferHelper = TestBed.get( SabloDeferHelper );
    converterService = TestBed.get( ConverterService );
    converterService.registerCustomPropertyHandler( "valuelist", new ValuelistConverter( sabloService, sabloDeferHelper) );
  });
  
  function createDefaultValuelist() {
      const data = [{displayValue: "abbba", realValue: 1}, {displayValue: "bbbbaab", realValue: 2}, {displayValue: "caaabbc", realValue: 3}]
      const json = {};
      json[VALUES] = data;
      json["valuelistid"] = 1073741880;
      return json;
  }

  it('should be created', inject([ValuelistConverter], (service: ValuelistConverter) => {
    expect(service).toBeTruthy();
  }));
  
  it( 'should convert from server to client', () => {
      let val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist() , "valuelist" )
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, "valuelist length should be '3' ");
      expect( val[0].displayValue).toBe( "abbba", "display value should be 'abbba'" );
      expect( val[0].realValue).toBe( 1, "real value should be '1'" );
      expect( val[2].displayValue).toBe( "caaabbc", "display value should be 'caaabbc'" );
      expect( val[2].realValue).toBe( 3, "real value should be '3'" );
  });
  
  it( 'should get display value', <any>fakeAsync(() => {
      let val: IValuelist = converterService.convertFromServerToClient(createDefaultValuelist() , "valuelist" )
      expect( val ).toBeDefined();
      expect( val.getDisplayValue).toBeDefined("should have 'getDisplayValue' function");
      
      let displayValue = undefined;
      val.getDisplayValue(4).then((val) => {
          displayValue = val;
      });
      expect(displayValue).not.toBeDefined( "display value should not be defined yet." );
      tick(100);//wait for the promise to be resolved
      expect(displayValue).toBe( "d", "display value should be 'd'" );
      
      //should be resolved right away
      val.getDisplayValue(4).then((val) => {
          displayValue = val;
      });
      expect(displayValue).toBe( "d", "display value should be 'd'" );
      
      let errorMessage = undefined;
      let realValue = 5;
      let display = undefined;
      val.getDisplayValue(realValue).then((val) => {
          display = val;
      }, (val) => {
          errorMessage = val;
      });
      expect(display).not.toBeDefined( "display value should not be defined yet." );
      expect(errorMessage).not.toBeDefined( "error message should not be defined yet." );
      tick(100);//wait for the promise to be resolved
      expect(display).not.toBeDefined("display value should not be defined" );
      expect(errorMessage).toBe("No display value found for "+realValue);
  }));
  
  it( 'should filter list', <any>fakeAsync(() => {
      let serverJSONValue = createDefaultValuelist();
      let val: IValuelist = converterService.convertFromServerToClient(serverJSONValue , "valuelist" )
      expect( val ).toBeDefined();
      expect( val.length ).toBe(3, "valuelist length should be '3' ");
      expect( val.filterList).toBeDefined("should have 'filter' function");
      
      let isFiltered:boolean = undefined;
      let promise = val.filterList("abb").then((val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined("filter promise should not be resolved yet.")
      
      let convertedValueForServer = converterService.convertFromClientToServer( val, "valuelist", val );
      expect(convertedValueForServer).toBeDefined();
      expect(convertedValueForServer.filter).toBe("abb", "valuelist filter should be 'abb'");
      expect(convertedValueForServer.id).toBe(1, "valuelist filter defer id should be '1'")
      expect(isFiltered).not.toBeDefined("filter promise should still not be resolved.")
      
      //convert once more, should have no changes
      convertedValueForServer = converterService.convertFromClientToServer( val, "valuelist", val );
      expect(convertedValueForServer).toBe(null, "convertedValueForServer should be 'null' because there is no change on the valuelist");
      
      //simulate answer from server
      serverJSONValue[HANDLED] = {id: 1, value: true};
      serverJSONValue[VALUES] = [{displayValue: "abbba", realValue: 1}, {displayValue: "caaabbc", realValue: 3}];
     
      val = converterService.convertFromServerToClient(serverJSONValue , "valuelist", val );
      tick(1);//a bit weird, but it seems we need to wait for the then function to be called..
      expect(isFiltered).toBe(true, "filter promise should be resolved.");
      expect( val.length ).toBe(2, "list is filtered, valuelist length should be '2' ");
      expect( val[0].displayValue).toBe( "abbba", "display value should be 'abbba'" );
      expect( val[0].realValue).toBe( 1, "real value should be '1'" );
      
      isFiltered = undefined;
      promise = val.filterList("xyz").then((val) => {
          isFiltered = true;
      });
      expect(isFiltered).not.toBeDefined("filter promise should not be resolved yet.")
      
      convertedValueForServer = converterService.convertFromClientToServer( val, "valuelist", val );
      expect(convertedValueForServer.filter).toBe("xyz", "valuelist filter should be 'xyz'");
      expect(convertedValueForServer.id).toBe(2, "valuelist filter defer id should be '2'")
      expect(isFiltered).not.toBeDefined("filter promise should still not be resolved.")
      
      serverJSONValue[HANDLED] = {id: 2, value: true};
      serverJSONValue[VALUES] = [];
     
      val = converterService.convertFromServerToClient(serverJSONValue , "valuelist", val );
      tick(1);
      expect( isFiltered ).toBe(true, "filter promise should be resolved.");
      expect( val.length ).toBe(0, "list is filtered, valuelist length should be '0' ");
      
      isFiltered = undefined;
      promise = val.filterList("x").then((val) => {
          isFiltered = true;
      }, (val) => { 
          isFiltered = false;
      });
      convertedValueForServer = converterService.convertFromClientToServer( val, "valuelist", val );
      expect(convertedValueForServer.filter).toBe("x", "valuelist filter should be 'x'");
      expect(convertedValueForServer.id).toBe(3, "valuelist filter defer id should be '3'");
      
      //assume the server did not filter the valuelist
      serverJSONValue[HANDLED] = {id: 3, value: false};
      val = converterService.convertFromServerToClient(serverJSONValue , "valuelist", val );
      tick(1);
      expect(isFiltered).toBe(false, "filter promise should be rejected.");      
  }));
});
