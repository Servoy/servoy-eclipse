import { TestBed } from '@angular/core/testing';
import { ConverterService } from '../../sablo/converter.service';
import { SabloService } from '../../sablo/sablo.service';
import { LoggerFactory, SessionStorageService, WindowRefService } from '@servoy/public';
import { ServicesService } from '../../sablo/services.service';
import { FoundsetType, FoundsetValue } from './foundset_converter';
import { SabloDeferHelper } from '../../sablo/defer.service';
import { ViewportService } from '../services/viewport.service';
import { DateType } from '../../sablo/converters/date_converter';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { IPropertyContext, PushToServerEnum, TypesRegistry } from '../../sablo/types_registry';
import { ObjectType } from '../../sablo/converters/object_converter';

describe('FoundsetConverter', () => {

    let converterService: ConverterService<FoundsetValue>
    let typesRegistry: TypesRegistry;
    let loggerFactory: LoggerFactory;
    let sabloService: SabloService;
    let viewportService: ViewportService;
    let sabloDeferHelper: SabloDeferHelper;
    let fs: FoundsetValue;
    let changeNotified = false;
    let someDate: Date;
    let someDateMs: any;
    let propertyContextWithReject: IPropertyContext;
    let propertyContextWithAllow: IPropertyContext;
    let propertyContextWithShallow: IPropertyContext;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports : [ServoyTestingModule],
            providers: [ConverterService, LoggerFactory,
                        WindowRefService, ServicesService, SessionStorageService, ViewportService, LoadingIndicatorService]
        });

        sabloService = TestBed.inject(SabloService);
        sabloService.connect({}, {}, '');
        sabloDeferHelper = TestBed.inject(SabloDeferHelper);
        viewportService = TestBed.inject(ViewportService);
        loggerFactory = TestBed.inject(LoggerFactory);
        converterService = TestBed.inject(ConverterService) as ConverterService<FoundsetValue>;
        typesRegistry = TestBed.inject(TypesRegistry);
        typesRegistry.registerGlobalType(FoundsetType.TYPE_NAME, new FoundsetType(sabloService, sabloDeferHelper, viewportService, loggerFactory));
        typesRegistry.registerGlobalType(DateType.TYPE_NAME_SABLO, new DateType());
        typesRegistry.registerGlobalType(ObjectType.TYPE_NAME, new ObjectType(typesRegistry, converterService), true);

        changeNotified = false;
        someDate = new Date();
        someDateMs = someDate.getTime();

        propertyContextWithReject = {
            getProperty: (_propertyName) => undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.REJECT,
            isInsideModel: true
        };
        propertyContextWithAllow = {
            getProperty: (_propertyName) => undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.ALLOW,
            isInsideModel: true
        };
        propertyContextWithShallow = {
            getProperty: (_propertyName) => undefined,
            getPushToServerCalculatedValue: () => PushToServerEnum.SHALLOW,
            isInsideModel: true
        };
    });

    // as property types reuse parts of JSON that comes from server, return a new instance each time to avoid meddling between separate tests due to this
    const createDefaultFoundset = () => {
        const json = {};
        json['foundsetId'] = 1;
        json['serverSize'] = 10;
        json['sortColumns'] = '';
        json['selectedRowIndexes'] = [0];
        json['multiSelect'] = false;
        json['hasMoreRows'] = true;
        const viewport = {};
        viewport['startIndex'] = 0;
        viewport['size'] = 5;
        viewport['rows'] = [{ _svyRowId: '5.ALFKI;_0' },
        { _svyRowId: '5.ANATR;_1' },
        { _svyRowId: '5.AROUT;_3' },
        { _svyRowId: '5.BERGS;_4' },
        { _svyRowId: '5.BLAUS;_5' }];
        json['viewPort'] = viewport;
        return json;
    };

    const getAndClearNotified = () => {
        const tm = changeNotified;
        changeNotified = false;
        return tm;
    };

    it('should convert from server to client', () => {
        fs = converterService.convertFromServerToClient(createDefaultFoundset(), typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME),
                undefined, undefined, undefined, propertyContextWithReject);
        expect(fs).toBeDefined();
        expect(fs.foundsetId).toBe(1, 'foundsetId should be \'1\' ');
        expect(fs.serverSize).toBe(10, 'foundset serverSize should be \'10');
        expect(fs.sortColumns).toBe('', 'foundset sort columns should be empty');
        expect(fs.multiSelect).toBe(false, 'foundset should not allow multiSelect');
        expect(fs.selectedRowIndexes.length).toBe(1, 'foundset should have \'1\' record selected ');
        expect(fs.selectedRowIndexes[0]).toBe(0, 'foundset should have first record selected');
        expect(fs.hasMoreRows).toBe(true, 'foundset should have more rows');
        expect(fs.viewPort).toBeDefined();
        expect(fs.viewPort.size).toBe(5, 'viewport size should be \'5\'');
        expect(fs.viewPort.startIndex).toBe(0, 'viewport startIndex should be \'0\'');
    });

    it('should not send change of int value to server when no pushToServer is specified for property', () => {
        const serverValue = {
            serverSize: 1,
            selectedRowIndexes: [],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 1,
                rows: [{
                    d: someDateMs, i: 1234, _svyRowId: '5.10643;_0'
                }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);

        // so no "w": false in server received value...
        const tmp = fs.viewPort.rows[0]['i'];
        fs.columnDataChanged(0, 'i', 4321234);
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);
        fs.viewPort.rows[0]['i'] = tmp;
    });

    it('Should get viewport size then request viewport and change selection', () => {
        // *** initial size no viewport
        const updateValue = {
            serverSize: 6,
            selectedRowIndexes: [0],
            multiSelect: false,
            viewPort:
            {
                startIndex: 0,
                size: 0,
                rows: []
            }
        };
        fs = converterService.convertFromServerToClient(updateValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        const copy: any = {};
        copy.serverSize = 6;
        copy.selectedRowIndexes = [0];
        copy.multiSelect = false;
        copy.viewPort = { startIndex: 0, size: 0, rows: [] };
        expect(fs.serverSize).toEqual(copy.serverSize);
        expect(fs.selectedRowIndexes).toEqual(copy.selectedRowIndexes);
        expect(fs.multiSelect).toEqual(copy.multiSelect);
        expect(fs.viewPort).toEqual(copy.viewPort);

        // *** request and receive new viewport (all records in this case)
        fs.loadRecordsAsync(0, 6);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        let updates = converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined)[0];
        expect(updates[0].newViewPort).toEqual({ startIndex: 0, size: 6 });
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        fs = converterService.convertFromServerToClient({
            upd_viewPort:
            {
                startIndex: 0,
                size: 6,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows:
                    [
                        { d: someDateMs, i: 1234, _svyRowId: '5.10643;_0' },
                        { d: someDateMs, i: 1234, _svyRowId: '5.10692;_1' },
                        { d: someDateMs, i: 1234, _svyRowId: '5.10702;_2' },
                        { d: null, i: 1234, _svyRowId: '5.10835;_3' },
                        { d: someDateMs, i: 1234, _svyRowId: '5.10952;_4' },
                        { d: someDateMs, i: 1234, _svyRowId: '5.11011;_5' }
                    ]
            },
            handledClientReqIds: [{
                id: updates[0]['id'],
                value: true
            }]
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 6;
        expectedfs.selectedRowIndexes = [0];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 6, rows: [] };
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10643;_0' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10692;_1' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10702;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: null, i: 1234, _svyRowId: '5.10835;_3' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10952;_4' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11011;_5' }));
        expect(fs.serverSize).toEqual(expectedfs.serverSize);
        expect(fs.selectedRowIndexes).toEqual(expectedfs.selectedRowIndexes);
        expect(fs.multiSelect).toEqual(expectedfs.multiSelect);
        expect(fs.viewPort).toEqual(jasmine.objectContaining(expectedfs.viewPort));

        // *** Selection change from Client
        fs.requestSelectionUpdate([1]); // WAS fs.selectedRowIndexes[0] = 1;
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        updates = converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined)[0];
        expect(updates[0].newClientSelectionRequest).toEqual([1], 'The selection change notification sent');
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        fs = converterService.convertFromServerToClient({ upd_selectedRowIndexes: [2], handledClientReqIds: [{
            id: updates[0]['id'],
            value: true
        }] }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);
        expect(fs.selectedRowIndexes.length).toBe(1);
        expect(fs.selectedRowIndexes[0]).toBe(2, 'should get selection update from server');
    });

    // eslint-disable-next-line max-len
    it('Should insert 2 before selection (server); this is a special case where foundset automatically expands viewport if viewport was showing whole foundset (an optimisation for scroll views)', () => {
        const serverValue = {
            serverSize: 6,
            selectedRowIndexes: [2],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 6,
                rows: [{ d: someDate, i: 1234, _svyRowId: '5.10643;_0' },
                { d: someDate, i: 1234, _svyRowId: '5.10692;_1' },
                { d: someDate, i: 1234, _svyRowId: '5.10702;_2' },
                { d: null, i: 1234, _svyRowId: '5.10835;_3' },
                { d: someDate, i: 1234, _svyRowId: '5.10952;_4' },
                { d: someDate, i: 1234, _svyRowId: '5.11011;_5' }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 8,
            upd_selectedRowIndexes: [4],
            upd_viewPort:
            {
                startIndex: 0,
                size: 8,
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: null, i: 1234, _svyRowId: '5.11078;_1' }],
                            startIndex: 1,
                            endIndex: 1,
                            type: 1
                        },
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: null, i: 1234, _svyRowId: '5.11078;_1' }],
                            startIndex: 1,
                            endIndex: 1,
                            type: 0
                        },
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.11079;_2' }],
                            startIndex: 2,
                            endIndex: 2,
                            type: 1
                        },
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.11079;_2' }],
                            startIndex: 2,
                            endIndex: 2,
                            type: 0
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 8;
        expectedfs.selectedRowIndexes = [4];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 8, rows: [] };
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10643;_0' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: null, i: 1234, _svyRowId: '5.11078;_1' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11079;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10692;_1' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10702;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: null, i: 1234, _svyRowId: '5.10835;_3' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10952;_4' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11011;_5' }));
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
        expect(fs.selectedRowIndexes[0]).toEqual(4);
    });

    it('Should remove the inserted 2 (server) and one more (1-3)', () => {

        const serverValue = {
            serverSize: 8,
            selectedRowIndexes: [4],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 8,
                rows: [{ d: someDate, i: 1234, _svyRowId: '5.10643;_0' },
                { d: null, i: 1234, _svyRowId: '5.11078;_1' },
                { d: someDate, i: 1234, _svyRowId: '5.11079;_2' },
                { d: someDate, i: 1234, _svyRowId: '5.10692;_1' },
                { d: someDate, i: 1234, _svyRowId: '5.10702;_2' },
                { d: null, i: 1234, _svyRowId: '5.10835;_3' },
                { d: someDate, i: 1234, _svyRowId: '5.10952;_4' },
                { d: someDate, i: 1234, _svyRowId: '5.11011;_5' }]
            }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 5,
            upd_selectedRowIndexes:
                [
                    1
                ],

            upd_viewPort:
            {
                startIndex: 0,
                size: 5,
                upd_rows:
                    [
                        { startIndex: 1, endIndex: 1, type: 2 },
                        { startIndex: 1, endIndex: 1, type: 2 },
                        { startIndex: 1, endIndex: 1, type: 2 }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 5;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 5, rows: [] };
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10643;_0' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10702;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: null, i: 1234, _svyRowId: '5.10835;_3' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10952;_4' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11011;_5' }));
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Foundset changed completely (relation & parent record changed for example on server - to something that is larger then we want to cache; so we will request smaller viewport)', () => {
        fs = converterService.convertFromServerToClient(createDefaultFoundset(), typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME),
                undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 12,
            upd_selectedRowIndexes: [0],
            upd_viewPort: { startIndex: 0, size: 0, rows: [] }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        expect(fs.serverSize).toEqual(12);
        expect(fs.selectedRowIndexes[0]).toEqual(0);
        expect(fs.viewPort.size).toEqual(0);
        expect(fs.viewPort.rows.length).toEqual(0);

        expect(getAndClearNotified()).toEqual(false);
        // *** request and receive new viewport (all except 3 records at the end)
        fs.loadRecordsAsync(0, 9);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        const updates = converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined)[0];
        expect(updates[0].newViewPort).toEqual({ startIndex: 0, size: 9 });
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        // *** viewport comes from server
        const rows = [
            { d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11107;_6' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11104;_7' },
            { d: someDateMs, i: 1234, _svyRowId: '5.11105;_8' }
        ];
        fs = converterService.convertFromServerToClient({
            upd_viewPort:
            {
                startIndex: 0,
                size: 9,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows
            },
            handledClientReqIds: [{
                id: updates[0]['id'],
                value: true
            }]
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        // we don't care too much about the state on the expectedfs, it has to be equal to the original one
        const expectedfs: any = {};
        expectedfs.serverSize = 12;
        expectedfs.selectedRowIndexes = [0];
        expectedfs.multiSelect = false;
        expectedfs.foundsetId = 1;
        expectedfs.sortColumns = '';
        expectedfs.hasMoreRows = true;
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows.map((value: any, _index: number, _array: []) => jasmine.objectContaining(value))};
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Should insert 2 at index 1 (now viewport stays the same as bounds but 2 get inserted and 2 from bottom get removed)', () => {
        const serverValue = {
            serverSize: 12,
            selectedRowIndexes: [4],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 9,
                rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11107;_6' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11104;_7' },
                { d: someDateMs, i: 1234, _svyRowId: '5.11105;_8' }
                ]
            }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 14,
            upd_selectedRowIndexes: [2],
            upd_viewPort:
            {
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDate, i: 1234, _svyRowId: '5.1112;_1' }],
                            startIndex: 0,
                            endIndex: 0,
                            type: 1
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDate, i: 1234, _svyRowId: '5.11112;_1' }],
                            startIndex: 0,
                            endIndex: 0,
                            type: 0
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDate, i: 1234, _svyRowId: '5.11113;_2' }],
                            startIndex: 1,
                            endIndex: 1,
                            type: 1
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDate, i: 1234, _svyRowId: '5.11113;_2' }],
                            startIndex: 1,
                            endIndex: 1,
                            type: 0
                        },
                        {
                            startIndex: 9,
                            endIndex: 10,
                            type: 2
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 14;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: [] };
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11112;_1' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11113;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDateMs, i: 1234, _svyRowId: '5.11107;_6' }));
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Should insert at last position (but still part of foundset)', () => {
        const rows = [{ d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11107;_6' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11104;_7' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11105;_8' }
        ];
        const serverValue = {
            serverSize: 14,
            selectedRowIndexes: [2],
            multiSelect: false,
            viewPort: { startIndex: 0, size: 9, rows: rows.slice() }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 15,
            upd_viewPort:
            {
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.11115;_29' }],
                            startIndex: 8,
                            endIndex: 8,
                            type: 1
                        },

                        {
                            startIndex: 9,
                            endIndex: 9,
                            type: 2
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.11115;_29' }],
                            startIndex: 8,
                            endIndex: 8,
                            type: 0
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 15;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        rows[8] = { d: someDate, i: 1234, _svyRowId: '5.11115;_29' };
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows.map((value: any, _index: number, _array: []) => jasmine.objectContaining(value))};
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Should delete last position of viewport (new record should be received in its place)', () => {
        const rows = [{ d: someDate, i: 1234, _svyRowId: '5.11112;_1' },
        { d: someDate, i: 1234, _svyRowId: '5.11113;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
        { d: someDate, i: 1234, _svyRowId: '5.11115;_29' }
        ];
        const serverValue = {
            serverSize: 15,
            selectedRowIndexes: [2],
            multiSelect: false,
            viewPort: { startIndex: 0, size: 9, rows: rows.slice() }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 14,
            upd_viewPort:
            {
                upd_rows:
                    [
                        {
                            startIndex: 8,
                            endIndex: 8,
                            type: 2
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDate, i: 1234, _svyRowId: '5.11106;_29' }],

                            startIndex: 8,
                            endIndex: 8,
                            type: 1
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 14;
        expectedfs.selectedRowIndexes = [2];
        expectedfs.multiSelect = false;
        rows[8] = { d: someDate, i: 1234, _svyRowId: '5.11106;_29' };
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows.map((value: any, _index: number, _array: []) => jasmine.objectContaining(value))};
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Should delete first position (new record should be received in its place)', () => {
        const rows = [{ d: someDate, i: 1234, _svyRowId: '5.11112;_1' },
        { d: someDate, i: 1234, _svyRowId: '5.11113;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
        { d: someDate, i: 1234, _svyRowId: '5.11106;_29' }
        ];
        const serverValue = {
            serverSize: 15,
            selectedRowIndexes: [2],
            multiSelect: false,
            viewPort: { startIndex: 0, size: 9, rows: rows.slice() }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithReject);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs = converterService.convertFromServerToClient({
            upd_serverSize: 13,
            upd_selectedRowIndexes: [1],
            upd_viewPort:
            {
                upd_rows:
                    [
                        {
                            startIndex: 0,
                            endIndex: 0,
                            type: 2
                        },

                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows:
                                [
                                    {
                                        d: someDateMs, i: 1234, _svyRowId: '5.11107;_29'
                                    }
                                ],
                            startIndex: 8,
                            endIndex: 8,
                            type: 1
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithReject);

        const expectedfs: any = {};
        expectedfs.serverSize = 13;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 9, rows: rows.map((value: any, _index: number, _array: []) => jasmine.objectContaining(value))};
        expectedfs.viewPort.rows.shift();
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.11107;_29' }));
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));
    });

    it('Should scroll down to bottom of foundset - viewport needs to be expanded )', () => {
        const rows = [{ d: someDate, i: 1234, _svyRowId: '5.11113;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.10350;_0' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11110;_1' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11111;_2' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11108;_3' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11109;_4' },
        { d: someDateMs, i: 1234, _svyRowId: '5.11106;_5' },
        { d: someDate, i: 1234, _svyRowId: '5.11106;_29' },
        { d: someDate, i: 1234, _svyRowId: '5.11107;_29' }
        ];
        const serverValue = {
            serverSize: 13,
            selectedRowIndexes: [1],
            multiSelect: false,
            viewPort: { startIndex: 0, size: 9, rows: rows.slice() }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithAllow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        fs.loadExtraRecordsAsync(4);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        const updates = converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined)[0];
        expect(updates[0].loadExtraRecords).toEqual(4);

        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        fs = converterService.convertFromServerToClient({
            upd_viewPort:
            {
                startIndex: 0,
                size: 13,
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10610;_9' },
                            { d: someDateMs, i: 1234, _svyRowId: '5.10631;_10' },
                            { d: someDateMs, i: 1234, _svyRowId: '5.10787;_11' },
                            { d: someDateMs, i: 1234, _svyRowId: '5.10832;_12' }
                            ],

                            startIndex: 9,
                            endIndex: 12,
                            type: 1
                        }
                    ]
            },
            handledClientReqIds: [{
                id: updates[0]['id'],
                value: true
            }]
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithAllow);

        const expectedfs: any = {};
        expectedfs.serverSize = 13;
        expectedfs.selectedRowIndexes = [1];
        expectedfs.multiSelect = false;
        expectedfs.viewPort = { startIndex: 0, size: 13, rows: rows.map((value: any, _index: number, _array: []) => jasmine.objectContaining(value))};
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10610;_9' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10631;_10' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10787;_11' }));
        expectedfs.viewPort.rows.push(jasmine.objectContaining({ d: someDate, i: 1234, _svyRowId: '5.10832;_12' }));
        expect(fs).toEqual(jasmine.objectContaining(expectedfs));

        // Should send change of date value to server
        const newDate: number = new Date().getTime() + 1;
        fs.columnDataChanged(12, 'd', newDate, someDateMs);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithAllow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.10832;_12', dp: 'd',
            value: converterService.convertFromClientToServer(newDate, typesRegistry.getAlreadyRegisteredType('Date'), undefined, undefined)[0] }, id: 1 }]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        // Should send change of int value to server
        fs.columnDataChanged(0, 'i', 4321);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithAllow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.11113;_2', dp: 'i', value: 4321 }, id: 1 }] // id for deferrs is always 1 in tests, does not increase - see TestSabloDeferHelper
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        // Should send sort to server
        fs.sort([{ name: 'i', direction: 'asc' }, { name: 'd', direction: 'desc' }]);
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        const message = converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithAllow)[0];
        expect(message).toEqual(
            [{ sort: [{ name: 'i', direction: 'asc' }, { name: 'd', direction: 'desc'}], id: 1 }] // id for deferrs is always 1 in tests, does not increase - see TestSabloDeferHelper
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);
    });

    it('Should detect shallow pushToServer cell changes and send them to server )', () => {
        // not sure if this scenario is of use in real life or we want to always rely on fs.columnDataChanged instead of proxies
        const rows = [  { d: someDate, i: 1234, _svyRowId: '5.11113;_2' },
                        { d: null, i: 1234, _svyRowId: '5.10350;_0' } ];
        const serverValue = {
            serverSize: 13,
            selectedRowIndexes: [1],
            multiSelect: false,
            viewPort: { startIndex: 0, size: 9, rows: rows.slice(),
                            _T: { mT: null, cT: { d: {_T: 'Date'} } } }
        };
        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME),
                undefined, undefined, undefined, propertyContextWithShallow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });

        // Should send change of previously non-null date value and previously null date value to server
        const newDate = new Date(new Date().getTime() + 1);
        fs.viewPort.rows[0].d = newDate;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.11113;_2', dp: 'd',
            value: converterService.convertFromClientToServer(newDate, typesRegistry.getAlreadyRegisteredType('Date'), undefined, undefined)[0] } }]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);

        fs.viewPort.rows[1].d = newDate;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.10350;_0', dp: 'd',
                value: converterService.convertFromClientToServer(newDate,
                    typesRegistry.getAlreadyRegisteredType('Date'), undefined, undefined)[0] } }]
        );
        expect(getAndClearNotified()).toEqual(false);
        expect(fs.getInternalState().hasChanges()).toEqual(false);
    });

    it('Should work when inserting one record above (it should not restore "original unproxied" value of record that slided down on top of the newly added record); proxies need to be up to date', () => {
        const serverValue = {
            serverSize: 5,
            selectedRowIndexes: [],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 2,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10643;_0' },
                    { d: someDateMs, i: 4321, _svyRowId: '5.34601;_1' }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithShallow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);
        
        // see that row proxies are in-place for shallow values to be sent to server automatically
        fs.viewPort.rows[1]['i'] = 4321234;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.34601;_1', dp: 'i', value: 4321234 } }]
        );

        // insert one row at index 0 in foundset prop.
        fs = converterService.convertFromServerToClient({
            upd_serverSize: 6,
            upd_selectedRowIndexes: [1],
            upd_viewPort:
            {
                startIndex: 0,
                size: 6,
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 9876, _svyRowId: '5.ATLKJ;_0' }],
                            startIndex: 0,
                            endIndex: 0,
                            type: 1
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithShallow);

        // see that data is ok
        expect(fs.viewPort.rows.length).toBe(3, 'model viewport size should be 6');
        expect(fs.viewPort.rows[0].i).toBe(9876, 'new rec value should be ok');
        expect(fs.viewPort.rows[1].i).toBe(1234, 'shifted to 1 rec value should be ok');
        expect(fs.viewPort.rows[2].i).toBe(4321234, 'shifted to 2 rec value should be ok');
        expect(getAndClearNotified()).toEqual(false);
        
        // see that proxies for rows that slided were updated
        fs.viewPort.rows[1]['i'] = 8888;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.10643;_0', dp: 'i', value: 8888 } }]
        );

        // see that new proxy was created
        fs.viewPort.rows[0]['i'] = 7777;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.ATLKJ;_0', dp: 'i', value: 7777 } }]
        );
    });

    it('Should work when deleting 2 records in the middle; proxies should be up to date', () => {
        const serverValue = {
            serverSize: 5,
            selectedRowIndexes: [],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 4,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10643;_0' },
                    { d: someDateMs, i: 4321, _svyRowId: '5.34601;_1' },
                    { d: someDateMs, i: 5678, _svyRowId: '5.11143;_2' },
                    { d: someDateMs, i: 3456, _svyRowId: '5.22201;_3' }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithShallow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);
        
        const recThatWillBeDeleted = fs.viewPort.rows[2];

        // delete two rows at index 1 in foundset prop.
        fs = converterService.convertFromServerToClient({
            upd_serverSize: 3,
            upd_selectedRowIndexes: [1],
            upd_viewPort:
            {
                startIndex: 0,
                size: 2,
                upd_rows:
                    [
                        {
                            startIndex: 1,
                            endIndex: 2,
                            type: 2
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithShallow);

        // see that data is ok
        expect(fs.viewPort.rows.length).toBe(2, 'model viewport size should be 6');
        expect(fs.viewPort.rows[0].i).toBe(1234, 'unaffected rec value should be ok');
        expect(fs.viewPort.rows[1].i).toBe(3456, 'shifted to 1 rec value should be ok');
        expect(getAndClearNotified()).toEqual(false);
        
        // see that proxies for rows that slided were updated
        fs.viewPort.rows[1]['i'] = 5555;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.22201;_3', dp: 'i', value: 5555 } }]
        );
        
        // see that proxies for deleted rows (in case component keeps obsolete references to them) are disabled
        recThatWillBeDeleted['i'] = 1357;
        expect(getAndClearNotified()).toEqual(false);
    });

    it('Should work when updating 1 record fully and 1 record partially; proxies should still work', () => {
        const serverValue = {
            serverSize: 5,
            selectedRowIndexes: [],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 4,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10643;_0' },
                    { d: someDateMs, i: 4321, _svyRowId: '5.34601;_1' },
                    { d: someDateMs, i: 5678, _svyRowId: '5.11143;_2' },
                    { d: someDateMs, i: 3456, _svyRowId: '5.22201;_3' }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithShallow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);
        
        const recThatWillBeFullyReplaced = fs.viewPort.rows[1];

        // delete two rows at index 1 in foundset prop.
        fs = converterService.convertFromServerToClient({
            upd_serverSize: 5,
            upd_selectedRowIndexes: [1],
            upd_viewPort:
            {
                startIndex: 0,
                size: 4,
                upd_rows:
                    [
                        {
                            _T: { mT: null, cT: { d: {_T: 'Date'} } },
                            rows: [{ d: someDateMs, i: 6565, _svyRowId: '5.WERTY;_1' }],
                            startIndex: 1,
                            endIndex: 1,
                            type: 0
                        },
                        {
                            rows: [{ i: 3434 }],
                            startIndex: 2,
                            endIndex: 2,
                            type: 0
                        }
                    ]
            }
        }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithShallow);

        // see that data is ok
        expect(fs.viewPort.rows.length).toBe(4, 'model viewport size should be 6');
        expect(fs.viewPort.rows[1].i).toBe(6565, 'fully changed rec value should be ok');
        expect(fs.viewPort.rows[2].i).toBe(3434, 'partially changed rec value should be ok');
        expect(getAndClearNotified()).toEqual(false);
        
        // see that proxies for rows that updated are ok
        fs.viewPort.rows[1]['i'] = 1111;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.WERTY;_1', dp: 'i', value: 1111 } }]
        );

        fs.viewPort.rows[2]['i'] = 2222;
        expect(getAndClearNotified()).toEqual(true);
        expect(fs.getInternalState().hasChanges()).toEqual(true);
        expect(converterService.convertFromClientToServer(fs, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, propertyContextWithShallow)[0]).toEqual(
            [{ viewportDataChanged: { _svyRowId: '5.11143;_2', dp: 'i', value: 2222 } }]
        );
        
        // the old record that was replaced by reference should no trigger send to server anymore, in case component keeps an obsolete reference to it
        recThatWillBeFullyReplaced['i'] = 7531;
        expect(getAndClearNotified()).toEqual(false);
    });

    it('Should disable old row proxies when updating the whole viewport from server', () => {
        const serverValue = {
            serverSize: 5,
            selectedRowIndexes: [],
            multiSelect: false,
            viewPort: {
                startIndex: 0,
                size: 4,
                _T: { mT: null, cT: { d: {_T: 'Date'} } },
                rows: [{ d: someDateMs, i: 1234, _svyRowId: '5.10643;_0' },
                    { d: someDateMs, i: 4321, _svyRowId: '5.34601;_1' },
                    { d: someDateMs, i: 5678, _svyRowId: '5.11143;_2' },
                    { d: someDateMs, i: 3456, _svyRowId: '5.22201;_3' }]
            }
        };

        fs = converterService.convertFromServerToClient(serverValue, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), undefined, undefined, undefined, propertyContextWithShallow);
        fs.getInternalState().setChangeListener((_doNotPush?: boolean) => {
            changeNotified = true;
        });
        expect(getAndClearNotified()).toEqual(false);
        
        const recThatWillBeFullyReplaced = fs.viewPort.rows[1];

        // delete two rows at index 1 in foundset prop.
        fs = converterService.convertFromServerToClient({
                upd_viewPort:
                {
                    startIndex: 0,
                    size: 1,
                    _T: { mT: null, cT: { d: {_T: 'Date'} } },
                    rows:
                        [
                            { d: someDateMs, i: 6565, _svyRowId: '5.WERTY;_1' }
                        ]
                }
            }, typesRegistry.getAlreadyRegisteredType(FoundsetType.TYPE_NAME), fs, undefined, undefined, propertyContextWithShallow);

        // the old record that was replaced by reference should no trigger send to server anymore, in case component keeps an obsolete reference to it
        recThatWillBeFullyReplaced['i'] = 7531;
        expect(getAndClearNotified()).toEqual(false);
    });

});
