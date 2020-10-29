import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { TestScheduler, } from 'rxjs/testing';
import { ServoyDefaultTypeahead } from './typeahead';
import { FormattingService, TooltipService, ServoyApi, Format} from '../../ngclient/servoy_public';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
import { SabloModule } from '../../sablo/sablo.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import {IValuelist} from '../../sablo/spectypes.service';
import { of } from 'rxjs';
import { FormsModule } from '@angular/forms';

const testScheduler = new TestScheduler((actual, expected) => {
  console.log('actual');
  console.log(actual);
  console.log('expected');
  console.log(expected);
  expect(actual).toEqual(expected);

});

const mockData = [
  {
    realValue: 1,
    displayValue: 'Bucuresti'
  },
  {
    realValue: 2,
    displayValue: 'Timisoara'
  },
  {
    realValue: 3,
    displayValue: 'Cluj'
  },
] as IValuelist;


describe('TypeaheadComponent', () => {
  let component: ServoyDefaultTypeahead;
  let fixture: ComponentFixture<ServoyDefaultTypeahead>;
  let servoyApi;

  beforeEach(async(() => {
    servoyApi = jasmine.createSpyObj( 'ServoyApi', ['getMarkupId', 'trustAsHtml']);
    mockData.hasRealValues = () => true;

    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultTypeahead ],
      providers: [ FormattingService , TooltipService],
      imports: [ServoyPublicModule, SabloModule, NgbModule, FormsModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultTypeahead);
    fixture.componentInstance.servoyApi = servoyApi as ServoyApi;

    component = fixture.componentInstance;
    component.valuelistID = mockData;
    component.dataProviderID = 3;
    component.format = new Format();
    component.format.type = 'NUMBER';
    component.format.display = '####';
    component.ngOnInit();

    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should set initial list of values', () => {
    expect(component.valuelistID.length).toBe(3);
  });

  it('should set initial dropdown closed', () => {
    expect(component.instance.isPopupOpen()).toBeFalsy();
  });

  it('should open dropdown on container click', fakeAsync(() => {
    fixture.detectChanges();
    component.click$.next('');
    tick(100);
    fixture.detectChanges();
    expect(component.instance.isPopupOpen()).toBeTruthy();
  }));


  it('should open dropdown on container focus', fakeAsync(() => {
    fixture.detectChanges();
    component.focus$.next('');
    tick(100);
    fixture.detectChanges();
    expect(component.instance.isPopupOpen()).toBeTruthy();
  }));

  it('should set initial list of values', (done) => {
    component.values(of('')).subscribe(values => {
      expect(values).toEqual(mockData);
      done();
    });
  });

  // it('should filter the list of values', (done) => {
  //   component.values(of('Bu')).subscribe(values => {
  //     expect(values).toEqual([mockData[0]]);
  //     done();
  //   });
  // });
});
