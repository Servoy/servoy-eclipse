import { TestBed, inject } from '@angular/core/testing';

import {EventEmitter } from '@angular/core';

import {WebsocketService} from './websocket.service';

import { FormService } from './form.service';

describe('FormService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [FormService, {provide: WebsocketService, useValue:new WebsocketServiceMock()}],
    });
  });

  it('should be created', inject([FormService], (service: FormService) => {
    expect(service).toBeTruthy();
  }));
});

class WebsocketServiceMock {
    public messages = new EventEmitter()
}