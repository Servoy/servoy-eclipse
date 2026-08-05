import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import { FormattingService, ServoyApi, TooltipService, ServoyPublicService,
         TooltipDirective, SabloTabseq } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;

  const servoyApi: any = { getMarkupId: vi.fn(), isInDesigner: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn(), getClientProperty: vi.fn() } as any;

    beforeEach(async () => {

    TestBed.configureTestingModule({
    declarations: [ServoyDefaultHtmlarea, TooltipDirective, SabloTabseq],
    imports: [FormsModule, EditorModule],
    providers: [FormattingService, TooltipService,
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' },
        { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl },
        provideHttpClient(withXhr(), withInterceptorsFromDi())]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHtmlarea);

    fixture.componentInstance.servoyApi = servoyApi;
    component = fixture.componentInstance;
    component.dataProviderID = 'WhatArea';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

});
