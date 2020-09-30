import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'

import { ServoyDefaultTextArea } from './textarea';
import { FormattingService, TooltipService} from '../../ngclient/servoy_public'
import { StartEditDirective } from "../../ngclient/utils/startedit.directive";
import { DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";
import { ServoyApi } from "../../ngclient/servoy_api";


describe("ServoyDefaultTextArea", () => {
  let component: ServoyDefaultTextArea;
  let fixture: ComponentFixture<ServoyDefaultTextArea>;
  let textArea: DebugElement;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTextArea, StartEditDirective],
      imports: [SabloModule, ServoyPublicModule],
      providers: [FormattingService,TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTextArea);
    textArea = fixture.debugElement.query(By.css('textarea')); 
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
      textArea.triggerEventHandler('focus', null);
      fixture.detectChanges();
      expect(component.servoyApi.startEdit).toHaveBeenCalled();
  });
  
  it('should have value test', () => {
    component.dataProviderID = 'test';
    fixture.detectChanges();
    expect(component.getNativeElement().value).toBe('test');
  });

  it('should have class: svy-textarea form-control form-control-sm input-sm svy-padding-xs', () => {
      expect( textArea.nativeElement.getAttribute('class')).toBe("svy-textarea form-control form-control-sm input-sm svy-padding-xs");
  });
  
});
