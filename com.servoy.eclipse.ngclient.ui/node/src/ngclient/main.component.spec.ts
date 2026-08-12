import { describe, it, expect, beforeEach, vi } from 'vitest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

describe('MainComponent', () => {
  const servicesService = { connect: vi.fn(), getSolutionSettings: vi.fn() } as any;
  servicesService.getSolutionSettings.mockReturnValue({ 
    sessionProblem: null
});
  const i18n: I18NListener = {
      messages: () =>i18n,
      destroy: () =>{}
  };
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        ServoyTestingModule
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
    }).overrideComponent(MainComponent, {
      set: { imports: [CommonModule, FormsModule], schemas: [CUSTOM_ELEMENTS_SCHEMA] }
    }).compileComponents();
  });
  it('should create the main component', async () => {
    const fixture = TestBed.createComponent(MainComponent);
    const app = fixture.debugElement.componentInstance;
    fixture.componentInstance.ngOnInit();
    expect(app).toBeTruthy();
    expect(servicesService.connect).toHaveBeenCalled();
  });
});
