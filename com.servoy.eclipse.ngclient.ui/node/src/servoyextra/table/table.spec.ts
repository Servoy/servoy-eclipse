import { ScrollingModule } from '@angular/cdk/scrolling';
import { Component, Input, ViewChild } from '@angular/core';
import { ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { FoundsetLinkedConverter } from '../../ngclient/converters/foundsetLinked_converter';
import { Foundset, FoundsetConverter } from '../../ngclient/converters/foundset_converter';
import { ViewportService } from '../../ngclient/services/viewport.service';
import { ServoyApi } from '../../ngclient/servoy_api';
import { ConverterService, PropertyContext } from '../../sablo/converter.service';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { LoggerFactory } from '../../sablo/logger.service';
import { SabloService } from '../../sablo/sablo.service';
import { ServicesService } from '../../sablo/services.service';
import { SpecTypesService, ViewPortRow } from '../../sablo/spectypes.service';
import { TestabilityService } from '../../sablo/testability.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { WindowRefService } from '../../sablo/util/windowref.service';
import { SessionStorageService } from '../../sablo/webstorage/sessionstorage.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyExtraTable } from './table';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'test-wrapper',
  template: '<div style="position: absolute; left: 29px; top: 139px; width: 571px; height: 438px;">\
                    <servoyextra-table #table [servoyApi]="servoyApi" [foundset]="foundset" [columns]="columns" [minRowHeight]="minRowHeight"\
                         [enableColumnResize]="enableColumnResize" [pageSize]="pageSize" [responsiveHeight]="responsiveHeight" [onCellClick]="cellClick" >\
                    </servoyextra-table > </div>',
})
class TestWrapperComponent {
    @ViewChild('table', { static: false }) table: ServoyExtraTable;
    @Input() servoyApi: ServoyApi;
    @Input() columns;
    @Input() foundset: Foundset;
    @Input() minRowHeight: string;
    @Input() enableColumnResize: boolean;
    @Input() pageSize: number;
    @Input() cellClick: (rowIdx: number, colIdx: number, record?: ViewPortRow, e?: MouseEvent, columnId?: string) => void;
    @Input() responsiveHeight;
}

describe('ServoyExtraTable', () => {
  let component: TestWrapperComponent;
  let fixture: ComponentFixture<TestWrapperComponent>;

  let converterService: ConverterService;
  let loggerFactory: LoggerFactory;
  let sabloService: SabloService;
  let sabloDeferHelper: SabloDeferHelper;
  let componentModelGetter: PropertyContext;

  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'isInDesigner', 'registerComponent', 'unRegisterComponent', 'isInAbsoluteLayout']);
  const onCellClick = jasmine.createSpy('onCellClick');

 const getFoundset  = (): Foundset => {
    const fs_json = {
      serverSize: 200,
      foundsetId: 1,
      sortColumns: '08d25c66d8b38adb872a5ffec31ca906 asc',
      selectedRowIndexes: [
        0
      ],
      multiSelect: false,
      hasMoreRows: true,
      viewPort: {
        startIndex: 0,
        size: 20,
        rows: [
          { _svyRowId: '5.10248;_0' },
          { _svyRowId: '5.10249;_1' },
          { _svyRowId: '5.10250;_2' },
          { _svyRowId: '5.10251;_3' },
          { _svyRowId: '5.10252;_4' },
          { _svyRowId: '5.10253;_5' },
          { _svyRowId: '5.10254;_6' },
          { _svyRowId: '5.10255;_7' },
          { _svyRowId: '5.10256;_8' },
          { _svyRowId: '5.10257;_9' },
          { _svyRowId: '5.10258;_10' },
          { _svyRowId: '5.10259;_11' },
          { _svyRowId: '5.10260;_12' },
          { _svyRowId: '5.10261;_13' },
          { _svyRowId: '5.10262;_14' },
          { _svyRowId: '5.10263;_15' },
          { _svyRowId: '5.10264;_16' },
          { _svyRowId: '5.10265;_17' },
          { _svyRowId: '5.10266;_18' },
          { _svyRowId: '5.10267;_19' },
          { _svyRowId: '5.10268;_20' }
        ]
      }
    };

    const fs = converterService.convertFromServerToClient(fs_json, 'foundset');
    componentModelGetter = (prop) => ({
      myfoundset: fs
    }[prop]);

    const fsl_json = {
      forFoundset: '08d25c66d8b38adb872a5ffec31ca906', vp: [10248, 10249, 10250, 10251, 10252, 10253, 10254, 10255, 10256, 10257, 10258, 10259,
        10260, 10261, 10262, 10263, 10264, 10265, 10266, 10267]
    };
    converterService.convertFromServerToClient(fsl_json, 'fsLinked', undefined, componentModelGetter);
    return fs;
  };

/** Finish initializing the virtual scroll component at the beginning of a test. */
const finishInit = () => {
  // On the first cycle we render and measure the viewport.
  fixture.detectChanges();
  flush();

  // On the second cycle we render the items.
  fixture.detectChanges();
  flush();

  // Flush the initial fake scroll event.
//  animationFrameScheduler.flush();
//  flush();
//  fixture.detectChanges();
};

  beforeEach(  () =>  {
    TestBed.configureTestingModule({
      declarations: [TestWrapperComponent, ServoyExtraTable],
      imports: [ServoyTestingModule, ScrollingModule],
      providers: [FoundsetLinkedConverter, FoundsetConverter, ConverterService, SabloService, TestabilityService, SpecTypesService, LoggerFactory,
        WindowRefService, ServicesService, SessionStorageService, ViewportService, LoadingIndicatorService]
    });

    sabloService = TestBed.inject(SabloService);
    sabloService.connect({}, {}, '');
    sabloDeferHelper = TestBed.inject(SabloDeferHelper);
    const viewportService = TestBed.inject(ViewportService);
    loggerFactory = TestBed.inject(LoggerFactory);
    converterService = TestBed.inject(ConverterService);
    converterService.registerCustomPropertyHandler('foundset', new FoundsetConverter(converterService, sabloService, sabloDeferHelper, viewportService, loggerFactory));
    converterService.registerCustomPropertyHandler('fsLinked', new FoundsetLinkedConverter(converterService, sabloService, viewportService, loggerFactory));
    servoyApi.isInAbsoluteLayout.and.callFake(() => false);

    fixture = TestBed.createComponent(TestWrapperComponent);
    fixture.componentInstance.servoyApi = servoyApi;
    component = fixture.componentInstance;
    component.foundset = getFoundset();
    component.columns = [
      {
        state: {
          allChanged: false, inNotify: false,
          conversionInfo: { dataprovider: 'fsLinked' },
          ignoreChanges: false, change: 0, hash: 2, changedKeys: {}, w: false, vEr: 5
        },
        format: { allowedCharacters: null, isMask: false, isRaw: false, edit: null, display: '#,##0.###', type: 'INTEGER', percent: '%', placeHolder: null, isNumberValidator: false },
        width: 'auto',
        autoResize: false,
        headerText: 'ID',
        showAs: 'text',
        dataprovider: [
          10248, 10249, 10250, 10251, 10252, 10253, 10254, 10255, 10256, 10257, 10258, 10259, 10260, 10261, 10262, 10263, 10264, 10265, 10266, 10267, 10268
        ],
        initialWidth: 'auto'
      },
      {
        state: {
          allChanged: false,
          inNotify: false,
          conversionInfo: {
            dataprovider: 'fsLinked'
          },
          ignoreChanges: false,
          change: 0,
          hash: 3,
          changedKeys: {},
          w: false,
          vEr: 5
        },
        format: { allowedCharacters: null, isMask: false, isRaw: false, edit: null, display: null, type: 'TEXT', placeHolder: null, isNumberValidator: false },
        width: 'auto',
        autoResize: false,
        headerText: 'Country',
        showAs: 'text',
        dataprovider: [
          'France', 'Germany', 'Brazil', 'France', 'Belgium', 'Brazil', 'Switzerland', 'Switzerland', 'Brazil', 'Venezuela', 'Austria',
          'Mexico', 'Germany', 'Brazil', 'USA', 'Austria', 'Sweden', 'France', 'Finland', 'Germany'],
        initialWidth: 'auto'
      },
      {
        state: {
          allChanged: false,
          inNotify: false,
          conversionInfo: { dataprovider: 'fsLinked' },
          ignoreChanges: false,
          change: 0,
          hash: 4,
          changedKeys: {},
          w: false,
          vEr: 5
        },
        format: { allowedCharacters: null, isMask: false, isRaw: false, edit: null, display: null, type: 'TEXT', placeHolder: null, maxLength: 15, isNumberValidator: false },
        width: 'auto',
        autoResize: false,
        headerText: 'City',
        showAs: 'text',
        dataprovider: [
          'Reims', 'Münster', 'Rio de Janeiro', 'Lyon', 'Charleroi', 'Rio de Janeiro', 'Bern', 'Genève', 'Resende', 'San Cristóbal', 'Graz',
          'México D.F.', 'Köln', 'Rio de Janeiro', 'Albuquerque', 'Graz', 'Bräcke', 'Strasbourg', 'Oulu', 'München'],
        initialWidth: 'auto'
      }
    ];
    component.minRowHeight = '25px';
    component.enableColumnResize = false;
    component.pageSize = 5;
    component.responsiveHeight = 500;
    component.cellClick = onCellClick;
  });


  it('should create table with 3 columns', fakeAsync(() => {
    finishInit();
    expect(component).toBeTruthy('table wrapper component should be created');
    expect(component.table).toBeTruthy('table component should be created');

    const compiled = fixture.debugElement.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('tr').length).toBeGreaterThan(0);

    console.log('----------------------------------DP: '+component.table.columns[0].dataprovider[1]);
    console.log('rendered rows '+component.table.renderedRows.length);

    const headers = component.table.getNativeElement().getElementsByTagName('th');
    expect(headers).toBeDefined();
    expect(headers.length).toEqual(3, 'should have 3 column headers');
    expect(headers[0].innerText.trim()).toEqual('ID', 'first header text should be ID');
    expect(headers[1].innerText.trim()).toEqual('Country', 'second header text should be Country');
    expect(headers[2].innerText.trim()).toEqual('City', 'third header text should be City');
    expect(component.table.getNativeElement().clientHeight).toBe(500);
    const rows = component.table.getNativeElement().getElementsByTagName('tr');
    expect(rows).toBeDefined();
    expect(rows.length).toBeGreaterThan(0, 'should have rows');
    const firstRow = rows[1].getElementsByTagName('td');
    expect(firstRow.length).toEqual(3, 'should have 3 columns');
    expect(firstRow[0].innerText).toEqual('10248');
    expect(firstRow[1].innerText).toEqual('France');
    expect(firstRow[2].innerText).toEqual('Reims');
  }));

  it('should call cell handlers', fakeAsync(() => {
    finishInit();
    const rows = component.table.getNativeElement().getElementsByTagName('tr');
    const firstRow = rows[1].getElementsByTagName('td');
    firstRow[1].click();
    fixture.detectChanges();
    flush();
    expect(onCellClick).toHaveBeenCalled();
    expect(onCellClick).toHaveBeenCalledWith(0, 1, { _svyRowId: '5.10248;_0' }, jasmine.anything(), undefined);
  }));
});
