import { Directive, Input} from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { MainComponent } from './main.component';
import {ServoyService} from './servoy.service';
import {AllServiceService} from './allservices.service';
import { FormService } from './form.service';
import { I18NProvider } from './servoy_public';
import { WebsocketService } from '../sablo/websocket.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';

describe('MainComponent', () => {
  const servicesService = jasmine.createSpyObj('ServoyService', ['connect']);
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        MainComponent,MockFormComponent,MockDefaultNavigator,MockSessionView
      ],
      providers:    [ {provide: ServoyService, useValue: servicesService },
        { provide:AllServiceService, useValue: {} },
        { provide:FormService, useValue: {} },
        { provide:I18NProvider, useValue: {} },
        { provide:WebsocketService, useValue: {} },
        { provide:LoadingIndicatorService, useValue: {}}]
    }).compileComponents();
  }));
  it('should create the main component', waitForAsync(() => {
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
@Directive({
    selector: 'session-view'
  })
  class MockSessionView{
  }
