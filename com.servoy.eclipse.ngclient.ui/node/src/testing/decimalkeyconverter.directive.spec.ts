import { TestBed, ComponentFixture, fakeAsync, tick, waitForAsync, inject } from '@angular/core/testing';
import { Component, Input, ViewChild, ElementRef, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { I18NProvider } from '../ngclient/services/i18n_provider.service';
import { LocaleService } from '../ngclient/locale.service';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from './servoytesting.module';

@Component({
    template: '<input type="text" [svyDecimalKeyConverter]="format" #element>'
})

class TestDecimalKeyConverterComponent {
    @Input() format;
    @ViewChild('element', { static: true }) elementRef: ElementRef;
}

describe('Directive: DecimalKeyConverter', () => {
    let component: TestDecimalKeyConverterComponent;
    let fixture: ComponentFixture<TestDecimalKeyConverterComponent>;
    let inputEl: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestDecimalKeyConverterComponent,
            ],
            imports: [ServoyTestingModule, ServoyPublicModule],
            providers: [I18NProvider,  LocaleService]
        });
        fixture = TestBed.createComponent(TestDecimalKeyConverterComponent);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should insert numpad decimal (nl == ,)', waitForAsync(inject([LocaleService], (locationService: LocaleService) => {
        component.format = {
            uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
            display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false
        };
        fixture.detectChanges();
        locationService.setLocale('nl', 'NL');
        locationService.isLoaded().then(() => {
            fixture.detectChanges();

            component.elementRef.nativeElement.value = '12';
            fixture.detectChanges();

            inputEl.triggerEventHandler('keydown', { keyCode: 110, which: 110 });
            fixture.detectChanges();

            expect(inputEl.nativeElement.value).toEqual('12,');
        });
    })));

    it ('should insert comma decimal (en == .)', fakeAsync(() => {
        component.format = {uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
                             display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false};
        fixture.detectChanges();

        component.elementRef.nativeElement.value = '12';
        fixture.detectChanges();
        tick();

        inputEl.triggerEventHandler('keydown', {keyCode: 110, which: 110});
        fixture.detectChanges();
        tick();
        expect(inputEl.nativeElement.value).toEqual('12.');
    }));
});
