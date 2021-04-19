import { ComponentFixture, TestBed, waitForAsync, tick, fakeAsync } from '@angular/core/testing';
import { ServoyApi } from '@servoy/public';
import { ServoyBootstrapLabel } from './label';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { runOnPushChangeDetection } from '../../testing';
import { By } from '@angular/platform-browser';

describe('ServoyBootstrapLabel', () => {
    let component: ServoyBootstrapLabel;
    let fixture: ComponentFixture<ServoyBootstrapLabel>;
    const servoyApi: jasmine.SpyObj<ServoyApi> = jasmine.createSpyObj<ServoyApi>('ServoyApi', ['getMarkupId', 'trustAsHtml', 'registerComponent', 'unRegisterComponent', 'registerComponent', 'unRegisterComponent']);
    let label;

    beforeEach(waitForAsync(() => {

        TestBed.configureTestingModule({
            declarations: [ServoyBootstrapLabel],
            providers: [],
            imports: [
                ServoyTestingModule, ServoyPublicModule]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyBootstrapLabel);
        label = fixture.debugElement.nativeElement;
        component = fixture.componentInstance;
        component.servoyApi = servoyApi;
        runOnPushChangeDetection(fixture);
        
        label = fixture.debugElement.query(By.css('span'));
    });

    it('should create', () => {
        expect(component).toBeDefined();
    });

    it('should render html', () => {
        component.showAs = 'trusted_html';
        component.text = '<div class="myclass" onclick="javascript:test()">hallo</div>';
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().children[0].innerHTML).toBe(component.text);
    });
    
    it('should not render html', () => {
        component.showAs = 'text';
        component.text = '<div class="myclass" onclick="javascript:test()">hallo</div>';
        runOnPushChangeDetection(fixture);
        expect(component.getNativeElement().children[0].innerHTML).toBe('&lt;div class="myclass" onclick="javascript:test()"&gt;hallo&lt;/div&gt;');
    });
    
     it('onaction, ondblclick and onrightclick handlers need to be called',  fakeAsync(() => {
        component.onActionMethodID = jasmine.createSpy('onActionMethodID');
        component.onRightClickMethodID = jasmine.createSpy('onRightClickMethodID');
        component.onDoubleClickMethodID = jasmine.createSpy('onDoubleClickMethodID');
        component.svyOnInit();
        label.triggerEventHandler('contextmenu', null);
        expect(component.onRightClickMethodID).toHaveBeenCalled();
        label.triggerEventHandler('click', null);
        tick(350);
        expect(component.onActionMethodID).toHaveBeenCalled();
        label.triggerEventHandler('dblclick', {target : label.nativeElement});
        expect(component.onDoubleClickMethodID).toHaveBeenCalled();
    }));
});
