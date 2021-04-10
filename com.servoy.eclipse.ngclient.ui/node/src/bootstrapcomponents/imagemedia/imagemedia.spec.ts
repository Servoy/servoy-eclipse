import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { runOnPushChangeDetection } from '../../testing';
import { ServoyBootstrapImageMedia } from './imagemedia';
import { By } from '@angular/platform-browser';
import { SimpleChange } from '@angular/core';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from 'servoy-public';

describe('ImagemediaComponent', () => {
    let component: ServoyBootstrapImageMedia;
    let fixture: ComponentFixture<ServoyBootstrapImageMedia>;
    let imageMedia;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapImageMedia],
             imports: [ServoyTestingModule, ServoyPublicModule],
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapImageMedia);
        imageMedia = fixture.debugElement.query(By.css('img'));
        component = fixture.componentInstance;
        component.servoyApi = jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml', 'registerComponent', 'unRegisterComponent']);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('onaction handler need to be called', () => {
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.svyOnInit();
        imageMedia.triggerEventHandler('keydown', { keyCode: 13 });
        expect(component.onActionMethodID).toHaveBeenCalled();
    });

    it('onaction handler need to be called', () => {
        let dpid = 'http://localhost:8080/';
        component.dataProviderID = dpid;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, dpid, true)
        });
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().src).toBe(dpid);
       
        let dpid2 = {url : 'http://localhost:8081/'};
        component.dataProviderID = dpid2;
        component.svyOnChanges({
            dataProviderID: new SimpleChange(null, dpid2, true)
        });
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().src).toBe(dpid2.url);
        
        let media = 'http://localhost:8082/';
        component.media = media;
        component.svyOnChanges({
            media: new SimpleChange(null, media, true)
        });
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().src).toBe(media);
    });

});
