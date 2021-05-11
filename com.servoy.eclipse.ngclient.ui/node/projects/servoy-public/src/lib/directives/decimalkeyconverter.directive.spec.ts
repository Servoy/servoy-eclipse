import { TestBed, ComponentFixture, fakeAsync, tick, waitForAsync, inject } from '@angular/core/testing';
import { Component, Input, ViewChild, ElementRef, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import * as  angularCommon from '@angular/common';
import { ServoyPublicTestingModule } from '../testing/publictesting.module';

@Component({
    template: '<input type="text" [svyDecimalKeyConverter]="format" #element>'
})

class TestDecimalKeyConverterComponent {
    @Input() format;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
}

const fixedSpyOn = <T>(target: T, prop: keyof T): jasmine.Spy => {
    const spy = jasmine.createSpy(`${prop}Spy`);
    spyOnProperty(target, prop).and.returnValue(spy);
    return spy;
};

describe('Directive: DecimalKeyConverter', () => {
    let component: TestDecimalKeyConverterComponent;
    let fixture: ComponentFixture<TestDecimalKeyConverterComponent>;
    let inputEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestDecimalKeyConverterComponent,
            ],
            imports: [ServoyPublicTestingModule],
            providers: []
        });
        fixture = TestBed.createComponent(TestDecimalKeyConverterComponent);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));

    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should insert numpad decimal (nl == ,)', waitForAsync(() => {
        fixedSpyOn(angularCommon, 'getLocaleNumberSymbol').and.returnValue(',');
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
    }));

    it('should insert comma decimal (en == .)', fakeAsync(() => {
        fixedSpyOn(angularCommon, 'getLocaleNumberSymbol').and.returnValue('.');

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
    }));
});
