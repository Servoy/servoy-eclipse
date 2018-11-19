import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By }              from '@angular/platform-browser';
import { ServoyDefaultCheck } from './check';
import {SabloModule} from "../../sablo/sablo.module";
import {
  DecimalkeyconverterDirective,
  FormatFilterPipe,
  FormattingService, StartEditDirective, ServoyApi,
  SvyFormat, TooltipDirective, TooltipService
} from "../../ngclient/servoy_public";
import {FormsModule} from "@angular/forms";
import {ServoyService} from "../../ngclient/servoy.service";

describe('CheckComponent', () => {
  let component: ServoyDefaultCheck;
  let fixture: ComponentFixture<ServoyDefaultCheck>;
  let servoyApi;
  let input;
  beforeEach(async(() => {
  servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCheck,TooltipDirective ],
      imports: [SabloModule, FormsModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheck);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    input = fixture.debugElement.query(By.css('input')).nativeElement;
    fixture.detectChanges();

  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('should click change value', () => {
      expect(input.checked).toBeFalsy(); // default state
      input.click();
      
      fixture.detectChanges();
      expect(input.checked).toBeTruthy(); // state after click
  });
  
  it('should getSelectionFromDP', () => {
      component.dataProviderID = 1;
      expect(component.getSelectionFromDataprovider()).toBeTruthy();
      
      component.dataProviderID = '1';
      expect(component.getSelectionFromDataprovider()).toBeTruthy();
      
      component.dataProviderID = 0;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
      
      component.dataProviderID = '0';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
      
      component.dataProviderID = '';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = 'something';
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
      
      component.dataProviderID = null;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();

      component.dataProviderID = undefined;
      expect(component.getSelectionFromDataprovider()).toBeFalsy();
  })
  
  
});
