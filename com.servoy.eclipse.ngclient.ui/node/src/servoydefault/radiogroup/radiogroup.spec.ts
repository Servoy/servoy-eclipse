import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultRadiogroup } from './radiogroup';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule, IValuelist } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { FormattingService, TooltipService } from '@servoy/public';
import { By } from '@angular/platform-browser';
import { NotNullOrEmptyPipe } from '@servoy/public';
import { DebugElement } from '@angular/core';

import {ChoiceElementDirective} from '../basechoice';

import { runOnPushChangeDetection } from '../../testing';

const mockData = [
                  {
                    realValue: 3,
                    displayValue: 'Bucharest'
                  },
                  {
                    realValue: 1,
                    displayValue: 'Timisoara'
                  },
                  {
                    realValue: 2,
                    displayValue: 'Amsterdam'
                  },
                ] as IValuelist;

describe('ServoyDefaultRadiogroup', () => {
  let component: ServoyDefaultRadiogroup;
  let fixture: ComponentFixture<ServoyDefaultRadiogroup>;
  let input: DebugElement;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultRadiogroup,ChoiceElementDirective],
      imports: [ServoyTestingModule, FormsModule, ServoyPublicModule],
      providers: [NotNullOrEmptyPipe, FormattingService, TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadiogroup);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    component.valuelistID = mockData;
    component.enabled = true;
    component.editable = true;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should click change value',() => {
      input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.checked).toBeFalsy(); // default state
      input.nativeElement.dispatchEvent(new Event('click'));
      runOnPushChangeDetection(fixture);
      expect(input.nativeElement.checked).toBeTruthy(); // state after click
    });

  it('should call itemClicked', () => {
      spyOn(component, 'itemClicked');
      input = fixture.debugElement.query(By.css('input'));
      input.nativeElement.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(component.itemClicked).toHaveBeenCalled();
  });

  it ('should be enabled', () => {
      input = fixture.debugElement.query(By.css('input'));
      expect(input.nativeElement.disabled).toBe(false);
  });
});
