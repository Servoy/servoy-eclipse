import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SabloModule } from '../../sablo/sablo.module'
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
import { ServoyDefaultImageMedia } from './imagemedia';
import { ServoyService } from '../../ngclient/servoy.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';
import { FormattingService,TooltipService, SvyUtilsService, ServoyApi} from '../../ngclient/servoy_public'
import { UploadDirective } from "../../ngclient/utils/upload.directive";
import { DebugElement, NgModule } from '@angular/core';
import { ApplicationService } from '../../ngclient/services/application.service';
import { By } from '@angular/platform-browser';
import { FileUploadWindowComponent } from '../../ngclient/services/file-upload-window/file-upload-window.component';
import { ViewportService } from '../../ngclient/services/viewport.service';

@NgModule({
  declarations: [FileUploadWindowComponent],
  entryComponents: [
    FileUploadWindowComponent,
  ]
})
class TestModule {}


describe("ServoyDefaultImageMedia", () => {
  let component: ServoyDefaultImageMedia;
  let fixture: ComponentFixture<ServoyDefaultImageMedia>;
  let imgUpload : DebugElement[];
  let applicationService: any;
  let servoyApi : any;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultImageMedia, UploadDirective],
      imports: [SabloModule, ServoyPublicModule],
      providers: [FormattingService,TooltipService, ApplicationService, ServoyService, I18NProvider, SvyUtilsService, {provide: ServoyApi, useValue: servoyApi}, ViewportService], 
    })
    .compileComponents();
  }));
  
  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultImageMedia);
    component = fixture.componentInstance;
    applicationService = jasmine.createSpyObj("ApplicationService", ["showFileOpenDialog"]);
    servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml", "getFormname"]); 
    component.servoyApi = servoyApi as ServoyApi;
    component.enabled = true; 
    component.editable = true;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  }); 

  it('should be the default image url', () => {
    expect(component.imageURL).toEqual('servoydefault/imagemedia/res/images/empty.gif');
  });

  it ('should delete the current uploaded file/image', () => {
    component.imageURL = "servoydefault/imagemedia/res/images/notemptymedia.gif";
    component.deleteMedia();
    expect(component.dataProviderID).toBeNull();
    expect(component.imageURL).toEqual('servoydefault/imagemedia/res/images/empty.gif');
  });

  // TODO: fix this test
  xit('should call the upload service', () => {
    imgUpload = fixture.debugElement.queryAll(By.css('img'));
    console.log(imgUpload);
    // try to open
    imgUpload[1].triggerEventHandler('click', null);
    fixture.detectChanges();
    expect(applicationService.showFileOpenDialog).toHaveBeenCalled();
  });
});

