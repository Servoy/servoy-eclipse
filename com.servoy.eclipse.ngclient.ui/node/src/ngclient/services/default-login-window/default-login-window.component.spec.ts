import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { LocalStorageService } from 'angular-web-storage';

import { SabloService } from '../../../sablo/sablo.service';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap'; 
import { DefaultLoginWindowComponent } from './default-login-window.component';

describe('DefaultLoginWindowComponent', () => {
  let component: DefaultLoginWindowComponent;
  let fixture: ComponentFixture<DefaultLoginWindowComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ FormsModule ],
      declarations: [ DefaultLoginWindowComponent ],
      providers: [ { provide: SabloService }, { provide: NgbActiveModal }, { provide: LocalStorageService }]
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
});
