import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {SimpleChange} from '@angular/core';
import { By } from '@angular/platform-browser';

import { ServoyDefaultLabel } from './label';
import { TooltipService, ComponentContributor, TooltipDirective, SabloTabseq,
         ImageMediaIdDirective, FormatFilterPipe, MnemonicletterFilterPipe,
         TrustAsHtmlPipe, FormattingService, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { runOnPushChangeDetection } from '../testingutils';

describe( 'SvLabel', () => {
    let component: ServoyDefaultLabel;
    let fixture: ComponentFixture<ServoyDefaultLabel>;
    let element: any;
    const servoyApi: any = { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

    beforeEach( async () => {
        TestBed.configureTestingModule( {
            declarations: [ServoyDefaultLabel, TooltipDirective, SabloTabseq,
                           ImageMediaIdDirective, FormatFilterPipe, MnemonicletterFilterPipe,
                           TrustAsHtmlPipe],
            providers: [TooltipService, FormattingService, ComponentContributor,
                        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }],
        } )
            .compileComponents();
    } );

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
        servoyApi.trustAsHtml.mockReturnValue( true );
        component.dataProviderID.set('<div class="myclass" onclick="javascript:test()">hallo</div>');
         runOnPushChangeDetection(fixture);
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( component.dataProviderID() );
    } );
    it( 'should not render html', () => {
        servoyApi.trustAsHtml.mockReturnValue( false );
        component.dataProviderID.set('<div class="myclass" onclick="javascript:test()">hallo</div>');
         runOnPushChangeDetection(fixture);
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( '<div class="myclass">hallo</div>' );
    } );

    it( 'should render markupid ', () => {
        servoyApi.getMarkupId.mockReturnValue( 'myid');
         runOnPushChangeDetection(fixture);
        expect(element.id).toBe('myid');
    } );

    it( 'should render mnemonic ', () => {
        fixture.componentRef.setInput('text', 'label');
        fixture.componentRef.setInput('mnemonic', 'l');
        component.ngOnChanges({
            mnemonic: new SimpleChange(null, 'l', false)
        });
         runOnPushChangeDetection(fixture);

        expect( component.child.nativeElement.children[1].innerHTML ).toBe('<u>l</u>abel');
        expect(element.getAttribute('accesskey')).toBe(component.mnemonic());
    } );

    it( 'should switch to labelFor', () => {
        fixture.componentRef.setInput('labelFor', true);
         runOnPushChangeDetection(fixture);
        const de = fixture.debugElement.query(By.css('label'));
        element = de.nativeElement;
        expect(element.tagName).toBe('LABEL');
    } );
} );
