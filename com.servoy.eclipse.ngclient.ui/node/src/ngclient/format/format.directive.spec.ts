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
    @ViewChild('element') elementRef:ElementRef;
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
    
    beforeEach(() => {
        fixture = TestBed.createComponent(TestTextfield);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));
        fixture.detectChanges();
        
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
       
        component.elementRef.nativeElement.focus();
        fixture.detectChanges();
        inputEl.triggerEventHandler("keydown", {"keyCode": "49"});
        fixture.detectChanges();
        tick(); 
        inputEl.triggerEventHandler("keypress", {"keyCode": "49"});  
        fixture.detectChanges();
        tick();
        
        //TODO this does not work yet because svyFormat is undefined in ngOnInit of the SvyFormat directive..
        //expect(component.elementRef.nativeElement.value).toEqual("15.04.2018");    
    }));
});
