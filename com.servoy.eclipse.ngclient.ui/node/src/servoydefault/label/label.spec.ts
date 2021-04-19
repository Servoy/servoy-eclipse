import { async, ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import {SimpleChange} from '@angular/core';
import { By } from '@angular/platform-browser';

import { ServoyDefaultLabel } from './label';
import { TooltipService, ComponentContributor, ServoyApi, FormattingService} from '../../ngclient/servoy_public';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';

import { runOnPushChangeDetection } from '../../testing';

describe( 'SvLabel', () => {
    let component: ServoyDefaultLabel;
    let fixture: ComponentFixture<ServoyDefaultLabel>;
    let element;
    const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent','registerComponent','unRegisterComponent']);

    beforeEach( waitForAsync(() => {
        TestBed.configureTestingModule( {
            declarations: [ServoyDefaultLabel],
            providers: [TooltipService, FormattingService, ComponentContributor],
            imports: [
                SabloModule, ServoyPublicModule],
        } )
            .compileComponents();
    } ) );

    beforeEach(() => {
        fixture = TestBed.createComponent( ServoyDefaultLabel );
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
         runOnPushChangeDetection(fixture);

        const de = fixture.debugElement.query(By.css('div'));
        element = de.nativeElement;
    } );

    it( 'should create', () => {
        expect( component ).toBeTruthy();
    } );

    it( 'should have called servoyApi.getMarkupId', () => {
        expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
    } );

    it( 'should render html', () => {
        servoyApi.trustAsHtml.and.returnValue( true );
        component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>';
         runOnPushChangeDetection(fixture);
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( component.dataProviderID );
    } );
    it( 'should not render html', () => {
        servoyApi.trustAsHtml.and.returnValue( false );
        component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>';
         runOnPushChangeDetection(fixture);
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( '<div class="myclass">hallo</div>' );
    } );

    it( 'should render markupid ', () => {
        servoyApi.getMarkupId.and.returnValue( 'myid');
         runOnPushChangeDetection(fixture);
        expect(element.id).toBe('myid');
    } );

    it( 'should render mnemonic ', () => {
        component.text = 'label';
        component.mnemonic = 'l';
        component.ngOnChanges({
            mnemonic: new SimpleChange(null, 'l', false)
        });
         runOnPushChangeDetection(fixture);

        expect( component.child.nativeElement.children[1].innerHTML ).toBe('<u>l</u>abel');
        expect(element.getAttribute('accesskey')).toBe(component.mnemonic);
    } );

    it( 'should switch to labelFor', () => {
        component.labelFor = true;
         runOnPushChangeDetection(fixture);
        const de = fixture.debugElement.query(By.css('label'));
        element = de.nativeElement;
        expect(element.tagName).toBe('LABEL');
    } );
} );
