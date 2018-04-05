import { async, ComponentFixture, TestBed, fakeAsync , tick} from '@angular/core/testing'
import {Component, Input, ElementRef, ViewChild} from '@angular/core'
import {SvyFormat} from './format.directive'

@Component({
    selector: 'test-textfield',
    template: '<input [svyFormat]="format"/>'
  })
class TestTextfield {
    @Input() format;
    @ViewChild('element') elementRef:ElementRef;
}

describe('SvyFormat', () => {
    let svyFormat : SvyFormat; 
    let component: TestTextfield;
    let fixture: ComponentFixture<TestTextfield>;

    beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        TestTextfield,
        SvyFormat
      ]
    });
  });

    function sendInput(text: string) {
        fixture.elementRef.nativeElement.value = text;
        fixture.elementRef.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
        return fixture.whenStable();
      }
    
    beforeEach(() => {
        fixture = TestBed.createComponent(TestTextfield);
        component = fixture.componentInstance;
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
        
        fixture.elementRef.nativeElement.dispatchEvent(new Event('blur'));//not sure if needed
        tick();
        
        //TODO expect(fixture.elementRef.nativeElement.value).toEqual("ABC");    
    }));
});

