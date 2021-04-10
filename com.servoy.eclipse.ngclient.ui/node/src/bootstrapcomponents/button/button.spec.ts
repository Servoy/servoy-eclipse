import { ComponentFixture, TestBed, waitForAsync, tick, fakeAsync } from '@angular/core/testing';
import { ServoyApi } from 'servoy-public';
import { ServoyBootstrapButton } from './button';
import { ServoyPublicModule } from 'servoy-public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { runOnPushChangeDetection } from '../../testing';
import { By } from '@angular/platform-browser';

describe('ServoyBootstrapButton', () => {
    let component: ServoyBootstrapButton;
    let fixture: ComponentFixture<ServoyBootstrapButton>;
    const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml', 'registerComponent', 'unRegisterComponent', 'registerComponent', 'unRegisterComponent']);
    let button;

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapButton],
            providers: [],
            imports: [
                ServoyTestingModule, ServoyPublicModule]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapButton);
        button = fixture.debugElement.nativeElement;
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
        runOnPushChangeDetection(fixture);
        
        button = fixture.debugElement.query(By.css('button'));
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should render html', () => {
        component.showAs = 'trusted_html';
        component.text = '<div class="myclass" onclick="javascript:test()">hallo</div>';
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().innerHTML).toBe(component.text);
    });
    
    it('should not render html', () => {
        component.showAs = 'text';
        component.text = '<div class="myclass" onclick="javascript:test()">hallo</div>';
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().innerHTML).toContain('&lt;div class="myclass" onclick="javascript:test()"&gt;hallo&lt;/div&gt;');
    });
    
     it('onaction, ondblclick and onrightclick handlers need to be called',  fakeAsync(() => {
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.onRightClickMethodID = jasmine.createSpy('onRightClickMethodID');
        component.onDoubleClickMethodID = jasmine.createSpy('onDoubleClickMethodID');
        component.svyOnInit();
        button.triggerEventHandler('contextmenu', null);
        expect(component.onRightClickMethodID).toHaveBeenCalled();
        button.triggerEventHandler('click', null);
        tick(350);
        expect(component.onActionMethodID).toHaveBeenCalled();
        button.triggerEventHandler('dblclick', {target : button.nativeElement});
        expect(component.onDoubleClickMethodID).toHaveBeenCalled();
    }));
});
