import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyPublicService, UploadDirective, FormattingService, TooltipService, ServoyApi,
         TooltipDirective, SabloTabseq, ImageMediaIdDirective } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { ServoyDefaultImageMedia } from './imagemedia';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('ServoyDefaultImageMedia', () => {
  let component: ServoyDefaultImageMedia;
  let fixture: ComponentFixture<ServoyDefaultImageMedia>;
  let imgUpload: DebugElement[];
  let servoyPublicService: any;
  let servoyApi: any;

  beforeEach(() => {

    servoyApi =  { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), getFormName: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any;

    TestBed.configureTestingModule({
        imports: [ServoyDefaultImageMedia, TooltipDirective, SabloTabseq,
                       ImageMediaIdDirective, UploadDirective],
        providers: [FormattingService, TooltipService, {provide: ServoyApi, useValue: servoyApi},
                    { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }],
      })
      .compileComponents();
    servoyPublicService = TestBed.inject(ServoyPublicService);
    fixture = TestBed.createComponent(ServoyDefaultImageMedia);
    
    component = fixture.componentInstance;
    component.servoyApi = servoyApi as ServoyApi;
    fixture.componentRef.setInput('enabled', true);
    fixture.componentRef.setInput('editable', true);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be the default image url', () => {
    expect(component.imageURL).toEqual(ServoyDefaultImageMedia.EMPTY);
  });

  it ('should delete the current uploaded file/image', () => {
    component.imageURL = ServoyDefaultImageMedia.NOT_EMPTY;
    imgUpload = fixture.debugElement.queryAll(By.css('.fa-times'));
    imgUpload[0].nativeElement.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(component.dataProviderID()).toBeNull();
    expect(component.imageURL).toEqual(ServoyDefaultImageMedia.EMPTY);
  });

  it('should call the upload service', () => {
    const spy = vi.spyOn(servoyPublicService, 'showFileOpenDialog').mockImplementation(() => {});
    imgUpload = fixture.debugElement.queryAll(By.css('.fa-upload'));
    imgUpload[0].nativeElement.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('should download file', () => {
      component.imageURL = ServoyDefaultImageMedia.NOT_EMPTY;
      const spy = vi.spyOn(component, 'downloadMedia');
      imgUpload = fixture.debugElement.queryAll(By.css('.fa-download'));
      imgUpload[0].nativeElement.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(spy).toHaveBeenCalled();
    });
});
