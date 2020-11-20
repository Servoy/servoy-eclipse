import { ComponentFixture, TestBed, fakeAsync, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { LocalStorageService } from '../../../sablo/webstorage/localstorage.service';

import { SabloService } from '../../../sablo/sablo.service';
import { NgbModule ,NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import { DefaultLoginWindowComponent } from './default-login-window.component';

describe('DefaultLoginWindowComponent', () => {
  let component: DefaultLoginWindowComponent;
  let fixture: ComponentFixture<DefaultLoginWindowComponent>;
  let sabloService;
  let localStorageService;
  let ngbActiveModal;
  beforeEach(waitForAsync(() => {
      sabloService = jasmine.createSpyObj('SabloService',['callService']);
      localStorageService = jasmine.createSpyObj('LocalStorageService',['set']);
      ngbActiveModal = jasmine.createSpyObj('NgbActiveModal',['close']);
    TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ DefaultLoginWindowComponent ],
      providers: [ { provide: SabloService,useValue:sabloService }, { provide: NgbActiveModal,useValue:ngbActiveModal }, { provide: LocalStorageService ,useValue:localStorageService }]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DefaultLoginWindowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should login', fakeAsync(() => {
      const loginData = { username: 'test', password: 'test', remember: true };
      const promise = Promise.resolve(loginData);
      sabloService.callService.and.returnValue(promise);
      component.username = 'test';
      component.password = 'test';
      component.doLogin();
      tick();
      expect(sabloService.callService).toHaveBeenCalledWith( 'applicationServerService', 'login',loginData,false);
      expect(localStorageService.set).toHaveBeenCalledTimes(2);
      expect(component.message).toBeUndefined();
    }));

  it('should not login',  fakeAsync(() => {
      const loginData = { username: 'test', password: 'test', remember: true };
      const promise = Promise.resolve(false);
      sabloService.callService.and.returnValue(promise);
      component.username = 'test';
      component.password = 'test';
      component.doLogin();
      tick();
      expect(sabloService.callService).toHaveBeenCalledWith( 'applicationServerService', 'login',loginData,false);
      expect(ngbActiveModal.close).toHaveBeenCalledTimes(0);
     expect(component.message).toBe( 'Invalid username or password, try again');
    }));
});
