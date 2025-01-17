import { TestBed, ComponentFixture, fakeAsync, tick, waitForAsync, inject } from '@angular/core/testing';
import { Component, Input, ViewChild, ElementRef, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ServoyPublicServiceTestingImpl, ServoyPublicTestingModule } from '../testing/publictesting.module';
import { ServoyPublicService } from '../services/servoy_public.service';

@Component({
    template: '<input type="text" [svyDecimalKeyConverter]="format" #element>',
    standalone: false
})
class TestDecimalKeyConverterComponent {
    @Input() format;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
}

describe('Directive: DecimalKeyConverter', () => {
    let component: TestDecimalKeyConverterComponent;
    let fixture: ComponentFixture<TestDecimalKeyConverterComponent>;
    let inputEl: DebugElement;
    const service = new ServoyPublicServiceTestingImpl();

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestDecimalKeyConverterComponent,
            ],
            imports: [ServoyPublicTestingModule],
            providers: [
                  { provide: ServoyPublicService, useValue: service }
            ]
        });
        fixture = TestBed.createComponent(TestDecimalKeyConverterComponent);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));

    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should insert numpad decimal (nl == ,)', fakeAsync(() => {
        service.setLocaleNumberSymbol(',');
        component.format = {
            uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
            display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false
        };
        fixture.detectChanges();

        component.elementRef.nativeElement.value = '12';
        fixture.detectChanges();

        inputEl.triggerEventHandler('keydown', { keyCode: 110, which: 110 });
        fixture.detectChanges();

        expect(inputEl.nativeElement.value).toEqual('12,');
        service.setLocaleNumberSymbol(null);
    }));

    it('should insert comma decimal (en == .)', fakeAsync(() => {
        service.setLocaleNumberSymbol('.');
        component.format = {
            uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
            display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false
        };
        fixture.detectChanges();

        component.elementRef.nativeElement.value = '12';
        fixture.detectChanges();
        tick();

        inputEl.triggerEventHandler('keydown', { keyCode: 110, which: 110 });
        fixture.detectChanges();
        tick();
        expect(inputEl.nativeElement.value).toEqual('12.');
        service.setLocaleNumberSymbol(null);
    }));
});
