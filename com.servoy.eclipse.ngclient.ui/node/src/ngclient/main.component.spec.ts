import { Directive, Input} from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';
import { MainComponent } from './main.component';
import {ServoyService} from './servoy.service';
import {AllServiceService} from './allservices.service';
import { FormService } from './form.service';
import { WebsocketService } from '../sablo/websocket.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { ServerDataService } from './services/serverdata.service';
import { I18NProvider } from './services/i18n_provider.service';
import { I18NListener } from '../../projects/servoy-public/src/lib/services/servoy_public.service';

describe('MainComponent', () => {
  const servicesService = jasmine.createSpyObj('ServoyService', ['connect']);
  const i18n: I18NListener = {
      messages: () =>i18n,
      destroy: () =>{}
  };
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        MainComponent,MockFormComponent,MockDefaultNavigator,MockSessionView
      ],
      providers:    [ {provide: ServoyService, useValue: servicesService },
        { provide:AllServiceService, useValue: {init: ()=>{}} },
        { provide:FormService, useValue: {} },
        { provide:I18NProvider, useValue: {
            listenForI18NMessages: () =>  i18n
        } },
        { provide:WebsocketService, useValue: {} },
        { provide:LoadingIndicatorService, useValue: {}},
        { provide:ServerDataService, useValue: {init: ()=>{}}}]
    }).compileComponents();
  }));
  it('should create the main component', waitForAsync(() => {
    const fixture = TestBed.createComponent(MainComponent);
    const app = fixture.debugElement.componentInstance;
    fixture.componentInstance.ngOnInit();
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
