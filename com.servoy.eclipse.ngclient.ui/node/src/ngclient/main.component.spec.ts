import { Directive, input } from '@angular/core';
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
import { ServoyTestingModule } from '../testing/servoytesting.module';
import { ServoyPublicModule } from '@servoy/public';

describe('MainComponent', () => {
  const servicesService = jasmine.createSpyObj('ServoyService', ['connect','getSolutionSettings']);
  servicesService.getSolutionSettings.and.returnValue({ 
    sessionProblem: null
});
  const i18n: I18NListener = {
      messages: () =>i18n,
      destroy: () =>{}
  };
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [
        MainComponent,MockFormComponent,MockDefaultNavigator,MockSessionView
      ],
      imports: [
        ServoyTestingModule, ServoyPublicModule
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
    selector: 'svy-form',
    standalone: false
})
  class MockFormComponent{
    public readonly name = input<string>(undefined);
  }

@Directive({
    selector: 'svy-default-navigator',
    standalone: false
})
  class MockDefaultNavigator{
    public readonly name = input<string>(undefined);
  }
@Directive({
    selector: 'session-view',
    standalone: false
})
  class MockSessionView{
  }
