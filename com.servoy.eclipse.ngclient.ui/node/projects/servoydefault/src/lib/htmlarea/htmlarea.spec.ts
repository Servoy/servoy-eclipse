import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ServoyDefaultHtmlarea  } from './htmlarea';
import { ServoyPublicTestingModule, FormattingService, ServoyApi, TooltipService } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';

describe('HtmlareaComponent', () => {
  let component: ServoyDefaultHtmlarea;
  let fixture: ComponentFixture<ServoyDefaultHtmlarea>;

  const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'isInDesigner','registerComponent','unRegisterComponent', 'getClientProperty']);

    beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
    declarations: [ServoyDefaultHtmlarea],
    imports: [EditorModule, ServoyPublicTestingModule, FormsModule],
    providers: [FormattingService, TooltipService,
        { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }, provideHttpClient(withInterceptorsFromDi())]
})
    .compileComponents();
  }));

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
