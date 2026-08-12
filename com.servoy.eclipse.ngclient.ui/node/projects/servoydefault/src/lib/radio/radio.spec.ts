import { describe, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultRadio } from './radio';
import { TooltipDirective, SabloTabseq, ServoyPublicService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';

describe('ImageLabelComponent', () => {
  let component: ServoyDefaultRadio;
  let fixture: ComponentFixture<ServoyDefaultRadio>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultRadio, TooltipDirective, SabloTabseq, FormsModule],
      providers: [{ provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultRadio);
    component = fixture.componentInstance;
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    fixture.detectChanges();
  });

  it.skip('should create', () => {
    expect(component).toBeTruthy();
  });
});
