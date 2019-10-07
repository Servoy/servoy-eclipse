import { async, ComponentFixture, TestBed, fakeAsync , tick} from '@angular/core/testing';
import {Component, Input, ElementRef, ViewChild, DebugElement} from '@angular/core';
import {By} from "@angular/platform-browser";
import {SvyFormat} from './format.directive';

@Component({
    selector: 'test-textfield',
    template: '<input [svyFormat]="format" #element/>'
  })
class TestTextfield {
    @Input() format;
    @ViewChild('element', {static: true}) elementRef:ElementRef;
}

describe('SvyFormat', () => {
    let svyFormat : SvyFormat; 
    let component: TestTextfield;
    let fixture: ComponentFixture<TestTextfield>;
    let inputEl: DebugElement;

beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        TestTextfield,
        SvyFormat
      ]
    });
  });

    function sendInput(text: string) {
        component.elementRef.nativeElement.value = text;
        component.elementRef.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        return fixture.whenStable();
      }
    
    function typeChar(key: string) {
        let char = key.charCodeAt(0);
        inputEl.triggerEventHandler("keydown", {"keyCode": char});
        fixture.detectChanges();
        tick(); 
        inputEl.triggerEventHandler("keypress", {"keyCode": char});  
        fixture.detectChanges();
        tick();
    }
    
    beforeEach(() => {
        fixture = TestBed.createComponent(TestTextfield);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));
        //DO NOT CALL fixture.detectChanges() before setting the format value, otherwise it calls ngOnInit without having the format set
        
    });
    
    it('should create', () => {
        expect(component).toBeTruthy();
    });
    
    it ('should apply uppercase format', fakeAsync(() => {
        component.format = {"uppercase":true,"allowedCharacters":null,"isMask":false,"isRaw":false,"edit":null,"display":null,"type":"TEXT","placeHolder":null,"isNumberValidator":false};
        fixture.detectChanges();
        
        sendInput("abc");
        tick();
        
        expect(inputEl.nativeElement.value).toEqual("ABC");    
    }));
    
    it ('should apply dd.MM.yyyy mask', fakeAsync(() => {
        component.format = {"allowedCharacters":null,"isMask":true,"isRaw":false,"edit":"##.##.####","display":"dd.MM.yyyy","type":"DATETIME","placeHolder":"dd.MM.yyyy","isNumberValidator":false};
        fixture.detectChanges();
        
        sendInput("05.04.2018");
        tick();
        expect(component.elementRef.nativeElement.value).toEqual("05.04.2018");    
       
        component.elementRef.nativeElement.selectionStart = 0;
        component.elementRef.nativeElement.selectionEnd = 0;
        fixture.detectChanges();
        "12032017".split("").map(function(c, i, array) { 
            typeChar(c);
        });        
        expect(component.elementRef.nativeElement.value).toEqual("12.03.2017");    
    }));
    
    it ('should apply 00.00 mask', fakeAsync(() => {
        component.format = {"allowedCharacters":null,"isMask":true,"isRaw":false,"edit":"00.00","display":"00.00","type":"NUMBER","placeHolder":"00.00","isNumberValidator":false};
        fixture.detectChanges();
        
        sendInput("5");
        tick();
        expect(component.elementRef.nativeElement.value).toEqual("50.00");
       
        component.elementRef.nativeElement.selectionStart = 1;
        component.elementRef.nativeElement.selectionEnd = 1;
        fixture.detectChanges();
        typeChar("1");
        expect(component.elementRef.nativeElement.value).toEqual("51.00");    
        
        component.elementRef.nativeElement.selectionStart = 0;
        component.elementRef.nativeElement.selectionEnd = 0;
        fixture.detectChanges();
        "2005".split("").map(function(c, i, array) { 
            typeChar(c);
        });
        expect(component.elementRef.nativeElement.value).toEqual("20.05");
    }));
});
