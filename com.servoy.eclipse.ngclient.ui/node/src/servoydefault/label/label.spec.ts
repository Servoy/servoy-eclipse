import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {SimpleChange} from '@angular/core';
import { By }              from '@angular/platform-browser';

import { ServoyDefaultLabel } from './label';

import {FormatFilterPipe,MnemonicletterFilterPipe} from '../../ngclient/servoy_public'

import { SabloModule } from '../../sablo/sablo.module'

describe( 'SvLabel', () => {
    let component: ServoyDefaultLabel;
    let fixture: ComponentFixture<ServoyDefaultLabel>;
    let element;

    beforeEach( async(() => {
        TestBed.configureTestingModule( {
            declarations: [ServoyDefaultLabel ,FormatFilterPipe,MnemonicletterFilterPipe],
            imports: [
                SabloModule],
        } )
            .compileComponents();
    } ) );

    beforeEach(() => {
        fixture = TestBed.createComponent( ServoyDefaultLabel );
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj( "ServoyApi", ["getMarkupId", "trustAsHtml"] );
        fixture.detectChanges();
        
        const de = fixture.debugElement.query(By.css('div'));
        element= de.nativeElement;
    } );

    it( 'should create', () => {
        expect( component ).toBeTruthy();
    } );

    it( 'should have called servoyApi.getMarkupId', () => {
        expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
    } );

    it( 'should render html', () => {
        component.servoyApi.trustAsHtml.and.returnValue( true )
        component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>'
        fixture.detectChanges();
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( component.dataProviderID )
    } );
    it( 'should not render html', () => {
        component.servoyApi.trustAsHtml.and.returnValue( false )
        component.dataProviderID = '<div class="myclass" onclick="javascript:test()">hallo</div>'
        fixture.detectChanges();
        expect( component.child.nativeElement.children[1].innerHTML ).toBe( '<div class="myclass">hallo</div>' )
    } );
    
    it( 'should render markupid ', () => {
        component.servoyApi.getMarkupId.and.returnValue( "myid")
        fixture.detectChanges();
        expect(element.id).toBe("myid");
    } );
    
    it( 'should render mnemonic ', () => {
        debugger;
        component.text = "label";
        component.mnemonic = "l";
        component.ngOnChanges({
            mnemonic: new SimpleChange(null, 'l', false)
        });
        fixture.detectChanges();
        
        expect( component.child.nativeElement.children[1].innerHTML ).toBe('<u>l</u>abel')
        expect(element.getAttribute("accesskey")).toBe(component.mnemonic);
    } );
    
    it( 'should switch to labelFor', () => {
        component.labelFor = true;
        fixture.detectChanges();
        const de = fixture.debugElement.query(By.css('label'));
        element= de.nativeElement;
        expect(element.tagName).toBe("LABEL");
    } );
} );
