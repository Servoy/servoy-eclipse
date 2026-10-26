import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyCoreFormContainer } from './formcontainer';
import { FormCache, PartCache } from '../../ngclient/types';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ServoyCoreFormContainer', () => {
  let component: ServoyCoreFormContainer;
  let fixture: ComponentFixture<ServoyCoreFormContainer>;
  let formServiceMock: jasmine.SpyObj<FormService>;

  beforeEach(() => {
    formServiceMock = jasmine.createSpyObj('FormService', ['getFormCacheByName']);
    TestBed.configureTestingModule({
      declarations: [ ServoyCoreFormContainer ],
      imports: [ServoyTestingModule, ServoyPublicModule, NoopAnimationsModule],
      providers: [ { provide: FormService, useValue: formServiceMock} ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreFormContainer);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getContainerStyle overflow (SVY-21655)', () => {
    const bodyLayout = { position: 'absolute', 'overflow-x': 'hidden', 'overflow-y': 'hidden' };
    const otherLayout = { position: 'absolute', overflow: 'hidden' };

    function formCacheWithParts(parts: PartCache[]): FormCache {
      const formCache = new FormCache('testform', { width: 640, height: 490 }, false, '', null);
      parts.forEach((part) => formCache.addPart(part));
      return formCache;
    }

    it('should apply the scrollbars of the body part when it is the only part', () => {
      formServiceMock.getFormCacheByName.and.returnValue(formCacheWithParts([new PartCache('body', [], bodyLayout)]));

      const style = component.getContainerStyle('testform');

      expect(style['overflowX']).toBe('hidden');
      expect(style['overflowY']).toBe('hidden');
    });

    it('should apply the scrollbars of the body part when header and footer parts exist', () => {
      formServiceMock.getFormCacheByName.and.returnValue(
        formCacheWithParts([
          new PartCache('titleheader', [], otherLayout),
          new PartCache('header', [], otherLayout),
          new PartCache('body', [], bodyLayout),
          new PartCache('footer', [], otherLayout),
        ]),
      );

      const style = component.getContainerStyle('testform');

      expect(style['overflowX']).toBe('hidden');
      expect(style['overflowY']).toBe('hidden');
    });
  });
});
