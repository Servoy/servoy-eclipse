import { Directive, Input} from '@angular/core';
import { TestBed, async } from '@angular/core/testing';
import { MainComponent } from './main.component';
import {ServoyService} from './servoy.service';
import {AllServiceService} from './allservices.service';
import { FormService } from './form.service';

describe('MainComponent', () => {
  const servicesService = jasmine.createSpyObj('ServoyService', ['connect']);
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        MainComponent,MockFormComponent,MockDefaultNavigator
      ],
      providers:    [ {provide: ServoyService, useValue: servicesService },
        { provide:AllServiceService, useValue: {} },
        { provide:FormService, useValue: {} }]
    }).compileComponents();
  }));
  it('should create the main component', async(() => {
    const fixture = TestBed.createComponent(MainComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
    expect(servicesService.connect).toHaveBeenCalled();
  }));
});

@Directive({
    selector: 'svy-form'
  })
  class MockFormComponent{
    @Input('name')
    public name: string;
  }

@Directive({
    selector: 'svy-default-navigator'
  })
  class MockDefaultNavigator{
    @Input('name')
    public name: string;
  }
