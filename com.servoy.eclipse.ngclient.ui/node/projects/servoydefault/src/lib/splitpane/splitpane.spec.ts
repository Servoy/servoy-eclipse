import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyDefaultSplitpane } from './splitpane';
import { FormattingService, TooltipService, ServoyPublicService, SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { BGSplitter } from './bg_splitter/bg_splitter.component';
import { BGPane } from './bg_splitter/bg_pane.component';

describe('ServoyDefaultSplitpane', () => {
  let component: ServoyDefaultSplitpane;
  let fixture: ComponentFixture<ServoyDefaultSplitpane>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [ServoyDefaultSplitpane, SabloTabseq, BGSplitter, BGPane],
      providers: [FormattingService, TooltipService,
                  { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultSplitpane);
    component = fixture.componentInstance;
    component.servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
