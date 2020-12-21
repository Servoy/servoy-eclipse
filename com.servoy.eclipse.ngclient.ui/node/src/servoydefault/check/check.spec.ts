import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By }              from '@angular/platform-browser';
import { ServoyDefaultCheck } from './check';
import {SabloModule} from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService} from '../../ngclient/servoy_public';
import {FormsModule} from '@angular/forms';

describe('CheckComponent', () => {
  let component: ServoyDefaultCheck;
  let fixture: ComponentFixture<ServoyDefaultCheck>;
  let servoyApi;
  let input; let label; let span;
  beforeEach(waitForAsync(() => {
  servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultCheck ],
      imports: [SabloModule, FormsModule, ServoyPublicModule],
      providers: [FormattingService, TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheck);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    component.text = 'Check me';
    component.enabled = true;
    component.editable = true;

    input = fixture.debugElement.query(By.css('input')).nativeElement;
    label = fixture.debugElement.query(By.css('label')).nativeElement;
    span = fixture.debugElement.query(By.css('span')).nativeElement;
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

      input.click();
      fixture.detectChanges();
      expect(input.checked).toBeFalsy();
  });

  it('should click on span and change value', fakeAsync(() => {

    expect(input.checked).toBeFalsy(); // default state
    clickOnElement(span, fixture, input,true);
    clickOnElement(span, fixture, input,false);
  }));

  it('should click on label and change', fakeAsync(() => {
    expect(input.checked).toBeFalsy(); // default state
    clickOnElement(label, fixture, input, true);
    clickOnElement(label, fixture, input, false);
  }));

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
  });

});

async function clickOnElement(element, fixture: ComponentFixture<ServoyDefaultCheck>, checkInput, toTestFlag) {
  element.click();
  fixture.detectChanges();
  tick();
  expect(checkInput.checked).toBe(toTestFlag);
}
