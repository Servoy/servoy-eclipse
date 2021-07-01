import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from '@servoy/public';

import { ServoyDefaultTextArea } from './textarea';
import { FormattingService, TooltipService} from '@servoy/public';
import { StartEditDirective } from '@servoy/public';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { runOnPushChangeDetection } from '../testingutils';

describe('ServoyDefaultTextArea', () => {
  let component: ServoyDefaultTextArea;
  let fixture: ComponentFixture<ServoyDefaultTextArea>;
  let textArea: DebugElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextArea, StartEditDirective],
      imports: [ServoyTestingModule, ServoyPublicModule, FormsModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextArea);
    textArea = fixture.debugElement.query(By.css('textarea'));
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should use start edit directive', () => {
      textArea.triggerEventHandler('focus', null);
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });

  it('should have value test', waitForAsync(() => {
    component.dataProviderID = 'test';
    runOnPushChangeDetection(fixture);
    fixture.whenStable().then(() =>
      expect(component.getNativeElement().value).toBe('test'));
  }));

  it('should call update method', () => {
    spyOn(component, 'pushUpdate');
    textArea.nativeElement.dispatchEvent(new Event('change'));
    expect(component.pushUpdate).toHaveBeenCalled();
  });

  it('should have class: svy-textarea form-control form-control-sm input-sm svy-padding-xs ng-untouched ng-pristine ng-valid', () => {
      expect( textArea.nativeElement.getAttribute('class')).toBe('svy-textarea form-control form-control-sm input-sm svy-padding-xs ng-untouched ng-pristine ng-valid');
  });

});
