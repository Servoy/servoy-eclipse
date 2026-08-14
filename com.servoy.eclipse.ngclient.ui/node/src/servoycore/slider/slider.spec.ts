import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from '@servoy/public';

import { ServoyCoreSlider } from './slider';
import { TooltipService } from '@servoy/public';

describe('ServoyCoreSlider', () => {
  let component: ServoyCoreSlider;
  let fixture: ComponentFixture<ServoyCoreSlider>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ServoyCoreSlider, ServoyTestingModule, ServoyPublicModule],
      providers: [TooltipService],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreSlider);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
