import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';

import { ServoyDefaultCheckGroup } from './checkgroup';
import { SabloModule } from '../../sablo/sablo.module';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { FormattingService, ServoyApi, TooltipService } from '../../ngclient/servoy_public';
import {FormsModule} from '@angular/forms';
import {NotNullOrEmptyPipe} from '../../ngclient/pipes/pipes';
import {By} from '@angular/platform-browser';
import { IValuelist } from '../../sablo/spectypes.service';

import {ChoiceElementDirective} from '../basechoice';

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

describe('ServoyDefaultCheckGroup', () => {
  let component: ServoyDefaultCheckGroup;
  let fixture: ComponentFixture<ServoyDefaultCheckGroup>;
  let servoyApi;

  beforeEach(waitForAsync(() => {
    servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent','registerComponent','unRegisterComponent']);
    mockData.hasRealValues = () => true;

    TestBed.configureTestingModule({
      declarations: [ServoyDefaultCheckGroup,ChoiceElementDirective],
      imports: [SabloModule, FormsModule, ServoyPublicModule],
      providers: [NotNullOrEmptyPipe, FormattingService, TooltipService]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultCheckGroup);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;
    component = fixture.componentInstance;
    component.valuelistID = mockData;
    component.enabled = true;
    component.editable = true;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  xit ('should set initial styles', () => {

  });

  xit('should click change value', () => {
    const input = fixture.debugElement.query(By.css('input')).nativeElement;
    const label = fixture.debugElement.query(By.css('label')).nativeElement;
    const span = fixture.debugElement.query(By.css('span')).nativeElement;

    expect(input.checked).toBeFalsy(); // default state
    input.click();

    fixture.detectChanges();
    expect(input.checked).toBeTruthy(); // state after click

    input.click();
    fixture.detectChanges();
    expect(input.checked).toBeFalsy();
  });

  it('should getSelectionFromDP', fakeAsync(() => {
    component.dataProviderID = 1;
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    tick();
    expect(component.getSelectedElements()[0]).toBe(component.dataProviderID);
  }));

  it('shoud getSelectionFromMultipleDp', fakeAsync(() => {
    component.dataProviderID = '1\n3';
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    tick();
    const selectedElements = component.getSelectedElements();
    expect(selectedElements).toContain(+component.dataProviderID[0]);
    expect(selectedElements).toContain(+component.dataProviderID[2]);
  }));

  it('should get DP from selection', fakeAsync(() => {
    component.dataProviderID = '1\n3';
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    tick();
    const selectedElements = component.getDataproviderFromSelection();
    expect(selectedElements).toContain(component.dataProviderID[0]);
    expect(selectedElements).toContain(component.dataProviderID[2]);
    expect(selectedElements.length).toBe(component.dataProviderID.length);
  }));


});
