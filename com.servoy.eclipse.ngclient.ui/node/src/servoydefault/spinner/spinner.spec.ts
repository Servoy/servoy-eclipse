import {async, ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ServoyDefaultSpinner} from './spinner';
import {SabloModule} from "../../sablo/sablo.module";
import {
  FormattingService, StartEditDirective, ServoyApi,
  SvyFormat, TooltipDirective, TooltipService
} from "../../ngclient/servoy_public";
import {FormsModule} from "@angular/forms";
import {NotNullOrEmptyPipe} from "../../ngclient/pipes/pipes";
import {By} from "@angular/platform-browser";
import {DebugElement} from "@angular/core";

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

fdescribe('ServoyDefaultCheckGroup', () => {
  let component: ServoyDefaultSpinner;
  let fixture: ComponentFixture<ServoyDefaultSpinner>;
  let buttonUp :any;let buttonDown: any;
  let servoyApi;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj("ServoyApi", ["getMarkupId"]);

    TestBed.configureTestingModule({
      declarations: [ServoyDefaultSpinner, SvyFormat, TooltipDirective],
      imports: [SabloModule, FormsModule],
      providers: [FormattingService, TooltipService]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSpinner);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    buttonUp = fixture.debugElement.queryAll(By.css('button'))[0];
    buttonDown = fixture.debugElement.queryAll(By.css('button'))[1];

    component = fixture.componentInstance;
    component.valuelistID = mockData;
    component.enabled = true;
    component.editable = true;
    component.dataProviderID = 1;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  xit('should click change value', () => {
    let input = fixture.debugElement.query(By.css('input')).nativeElement;
  });



  it('should got undefined dp if dp is not present in valuelist', () => {
    component.dataProviderID = 'Salut';
    let selection = component.getSelectionFromDataprovider();
    fixture.detectChanges();
    expect(selection).toBeFalsy();
  });

  it('should selection to match displayValue of dp 1', () => {
    let selection = component.getSelectionFromDataprovider();
    expect(selection).toBe("Timisoara");
  });

  it('should change dp when click the up button', fakeAsync( () => {
    buttonDown.nativeElement.click();
    fixture.detectChanges();
    tick();
    let selection = component.getSelectionFromDataprovider();
    expect(selection).toBe("Bucharest");
  }));

  it('should getSelectionFromDP', fakeAsync(() => {
    buttonUp.nativeElement.click();
    fixture.detectChanges();
    tick();
    let selection = component.getSelectionFromDataprovider();
    expect(selection).toBe("Amsterdam");
  }));


});
