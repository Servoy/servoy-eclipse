import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

import { ServoyDefaultTextField } from './textfield';
import { FormattingService, TooltipService} from '../../ngclient/servoy_public'
import { By } from "@angular/platform-browser";


describe("ServoyDefaultTextField", () => {
  let component: ServoyDefaultTextField;
  let fixture: ComponentFixture<ServoyDefaultTextField>;
  let textField;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextField],
      imports: [SabloModule, ServoyPublicModule],
      providers: [FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextField);
    textField = fixture.debugElement.query(By.css('input'))
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml", "startEdit"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });
  
  it('should use start edit directive', () => {
      textField.triggerEventHandler('focus', null);
      fixture.detectChanges();
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });
  
  it('should call update method', () => {
      spyOn(component, 'update');
      component.dataProviderID = 'test';
      fixture.whenStable().then(() => {
          expect(component.update).toHaveBeenCalled();
          expect(component.getNativeElement().value).toBe('component.valueBeforeChange');
        });
  });
  
  it('should call attachFocusListeners method', () => {
      expect(component.valueBeforeChange).toBe(undefined);
      spyOn(component, 'attachFocusListeners');
      textField.triggerEventHandler('focus', null);
      fixture.detectChanges();
      fixture.whenStable().then(() => {
          expect(component.attachFocusListeners).toHaveBeenCalled();
          expect(component.valueBeforeChange).toBe('test'); 
        });
  });
});
