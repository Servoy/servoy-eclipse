import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultPassword } from './password';
import { SabloModule } from "../../sablo/sablo.module";
import { FormsModule } from "@angular/forms";
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService} from "../../ngclient/servoy_public";
import { DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";
import { TooltipDirective } from "../../ngclient/tooltip/tooltip.directive";

describe('PasswordComponent', () => {
  let component: ServoyDefaultPassword;
  let fixture: ComponentFixture<ServoyDefaultPassword>;
  let servoyApi;
  let inputEl: DebugElement;
  let directiveInstance: TooltipDirective;

  beforeEach(async(() => {
      servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
      TestBed.configureTestingModule({
        declarations: [ServoyDefaultPassword, TooltipDirective],
        imports: [SabloModule, FormsModule, ServoyPublicModule],
        providers: [FormattingService, TooltipService]
      })
      .compileComponents();
    }));
  
  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultPassword);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should have called servoyApi.getMarkupId', () => {
      expect( component.servoyApi.getMarkupId ).toHaveBeenCalled();
  });

  it('should call update method', () => {
      spyOn(component, 'update');
      component.dataProviderID = 'test';
      fixture.whenStable().then(() => {
          expect(component.update).toHaveBeenCalled();
        });
  });
  
  it('should have a placeholder', () => {
      expect( component.placeholderText ).toBeUndefined();
      expect( component.toolTipText).toBeUndefined();
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      component.placeholderText = "placeholder";
      fixture.detectChanges();
      expect( inputEl.nativeElement.placeholder ).toEqual("placeholder");
  });
  
  it('should have a tooltip', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      directiveInstance = inputEl.injector.get(TooltipDirective);
      inputEl.nativeElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();
      expect(directiveInstance.isActive).toBe(false); // false because the text is undefined
      directiveInstance.tooltipText = "Hi";
      fixture.detectChanges();
      expect(directiveInstance.tooltipText).toBe('Hi');
  })
  
  it('should have class: svy-password form-control input-sm svy-padding-xs', () => {
      inputEl = fixture.debugElement.query(By.css('input'));
      fixture.detectChanges();
      expect( inputEl.nativeElement.getAttribute('class')).toBe("svy-password form-control input-sm svy-padding-xs");
  });
  
});
