import { DecimalkeyconverterDirective } from './decimalkeyconverter.directive';
import {TestBed, ComponentFixture,fakeAsync,tick} from '@angular/core/testing';
import {Component,Input,ViewChild,ElementRef,DebugElement} from '@angular/core';
import {SvyFormat} from '../format/format.directive';
import {By} from "@angular/platform-browser";
import * as numeral from 'numeral';

@Component({
    template: '<input type="text" [svyDecimalKeyConverter]="format" #element>'
})

class TestDecimalKeyConverterComponent {
    @Input() format;
    @ViewChild('element', {static: true}) elementRef:ElementRef;
}

describe('Directive: DecimalKeyConverter', () => {
    let component: TestDecimalKeyConverterComponent;
    let fixture: ComponentFixture<TestDecimalKeyConverterComponent>;
    let inputEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
              TestDecimalKeyConverterComponent,
              DecimalkeyconverterDirective
            ]
         });
        fixture = TestBed.createComponent(TestDecimalKeyConverterComponent);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));
    });
    
    it('should create', () => {
        expect(component).toBeTruthy();
    });
    
    it ('should insert numpad decimal', fakeAsync(() => {
        numeral.locale('en');
        component.format = {"uppercase":true,"allowedCharacters":null,"isMask":false,"isRaw":false,"edit":null,"display":null,"type":"NUMBER","placeHolder":null,"isNumberValidator":false};
        fixture.detectChanges();
        
        component.elementRef.nativeElement.value = "12";
        fixture.detectChanges();
        tick();
        
        inputEl.triggerEventHandler("keydown", {"keyCode": 110, "which": 110});
        fixture.detectChanges();
        tick();
        
        expect(inputEl.nativeElement.value).toEqual("12.");    
    }));
    
    it ('should insert comma decimal', fakeAsync(() => {
        numeral.locale('nl-nl');
        component.format = {"uppercase":true,"allowedCharacters":null,"isMask":false,"isRaw":false,"edit":null,"display":null,"type":"NUMBER","placeHolder":null,"isNumberValidator":false};
        fixture.detectChanges();
        
        component.elementRef.nativeElement.value = "12";
        fixture.detectChanges();
        tick();
        
        inputEl.triggerEventHandler("keydown", {"keyCode": 110, "which": 110});
        fixture.detectChanges();
        tick();
        expect(inputEl.nativeElement.value).toEqual("12,");    
    }));
});