import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyPublicTestingModule, UploadDirective, FormattingService, TooltipService, ServoyApi, ServoyPublicService } from '@servoy/public';
import { ServoyDefaultImageMedia } from './imagemedia';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('ServoyDefaultImageMedia', () => {
  let component: ServoyDefaultImageMedia;
  let fixture: ComponentFixture<ServoyDefaultImageMedia>;
  let imgUpload: DebugElement[];
  let servoyPublicService;
  let servoyApi: any;

  beforeEach(() => {

    servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'getFormName','registerComponent','unRegisterComponent']);

    TestBed.configureTestingModule({
        declarations: [ ServoyDefaultImageMedia, UploadDirective],
        imports: [ServoyPublicTestingModule],
        providers: [FormattingService, TooltipService, {provide: ServoyApi, useValue: servoyApi}],
      })
      .compileComponents();
    servoyPublicService = TestBed.inject(ServoyPublicService);
    fixture = TestBed.createComponent(ServoyDefaultImageMedia);
    
    component = fixture.componentInstance;
    component.servoyApi = servoyApi as ServoyApi;
    component.enabled = true;
    component.editable = true;
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
    expect(component.dataProviderID).toBeNull();
    expect(component.imageURL).toEqual(ServoyDefaultImageMedia.EMPTY);
  });

  it('should call the upload service', () => {
    const spy = spyOn(servoyPublicService, 'showFileOpenDialog');
    imgUpload = fixture.debugElement.queryAll(By.css('.fa-upload'));
    imgUpload[0].nativeElement.dispatchEvent(new Event('click'));
    fixture.detectChanges();
    expect(spy).toHaveBeenCalled();
  });

  it('should download file', () => {
      component.imageURL = ServoyDefaultImageMedia.NOT_EMPTY;
      const spy = spyOn(component, 'downloadMedia');
      imgUpload = fixture.debugElement.queryAll(By.css('.fa-download'));
      imgUpload[0].nativeElement.dispatchEvent(new Event('click'));
      fixture.detectChanges();
      expect(spy).toHaveBeenCalled();
    });
});

