import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyCoreFormContainer } from './formcontainer';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ServoyCoreFormContainer', () => {
  let component: ServoyCoreFormContainer;
  let fixture: ComponentFixture<ServoyCoreFormContainer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ServoyCoreFormContainer ],
      imports: [ServoyTestingModule, ServoyPublicModule, NoopAnimationsModule],
      providers: [ { provide: FormService, useValue: {getFormCacheByName: () => {} }} ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreFormContainer);
    component = fixture.componentInstance;
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
