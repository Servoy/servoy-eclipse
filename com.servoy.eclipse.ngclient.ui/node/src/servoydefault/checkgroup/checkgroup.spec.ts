import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ServoyDefaultCheckGroup} from './checkgroup';
import {SabloModule} from "../../sablo/sablo.module";
import {
  FormattingService, StartEditDirective, ServoyApi,
  SvyFormat, TooltipDirective, TooltipService
} from "../../ngclient/servoy_public";
import {FormsModule} from "@angular/forms";
import {NotNullOrEmptyPipe} from "../../ngclient/pipes/pipes";
import {By} from "@angular/platform-browser";

const mockData = [
                          {
                            realValue: 3,
                            displayValue: "Bucharest"
                          },
                          {
                            realValue: 1,
                            displayValue: "Timisoara"
                          },
                          {
                            realValue: 2,
                            displayValue: "Amsterdam"
                          },
                        ];

describe('ServoyDefaultCheckGroup', () => {
  let component: ServoyDefaultCheckGroup;
  let fixture: ComponentFixture<ServoyDefaultCheckGroup>;
  let servoyApi;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj("ServoyApi", ["getMarkupId", "trustAsHtml"]);

    TestBed.configureTestingModule({
      declarations: [ServoyDefaultCheckGroup, NotNullOrEmptyPipe, SvyFormat, StartEditDirective, TooltipDirective],
      imports: [SabloModule, FormsModule],
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
    let input = fixture.debugElement.query(By.css('input')).nativeElement;
    let label = fixture.debugElement.query(By.css('label')).nativeElement;
    let span = fixture.debugElement.query(By.css('span')).nativeElement;

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
    let selectedElements = component.getSelectedElements()
    expect(selectedElements).toContain(+component.dataProviderID[0]);
    expect(selectedElements).toContain(+component.dataProviderID[2]);
  }));

  it('should get DP from selection', fakeAsync(() => {
    component.dataProviderID = '1\n3';
    component.setSelectionFromDataprovider();
    fixture.detectChanges();
    tick();
    let selectedElements = component.getDataproviderFromSelection();
    expect(selectedElements).toContain(component.dataProviderID[0]);
    expect(selectedElements).toContain(component.dataProviderID[2]);
    expect(selectedElements.length).toBe(component.dataProviderID.length);
  }));


});
