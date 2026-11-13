import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DefaultNavigator } from './default-navigator';
import { FormService } from '../../ngclient/form.service';

describe('DefaultNavigator', () => {
  let component: DefaultNavigator;
  let fixture: ComponentFixture<DefaultNavigator>;
  let formService: any;
  let navigatorCache: any;

  beforeEach(async () => {
    navigatorCache = { name: 'svy_default_navigator', model: { currentIndex: 3, minIndex: 1, maxIndex: 10, hasMore: false } };
    formService = {
      getFormCacheByName: vi.fn().mockReturnValue({ getComponent: vi.fn().mockReturnValue(navigatorCache) }),
      executeEvent: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DefaultNavigator],
      providers: [{ provide: FormService, useValue: formService }],
    }).compileComponents();

    fixture = TestBed.createComponent(DefaultNavigator);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('name', 'testForm');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit should resolve the navigator cache and set the initial slider value', () => {
    component.ngOnInit();
    expect(formService.getFormCacheByName).toHaveBeenCalledWith('testForm');
    expect(component.navigatorComponentCache).toBe(navigatorCache);
    expect(component.sliderValue).toBe(-3);
  });

  it('setIndex should update the model, slider value and fire the server event', () => {
    component.ngOnInit();
    component.setIndex('5');
    expect(component.navigatorComponentCache.model.currentIndex).toBe(5);
    expect(component.sliderValue).toBe(-5);
    expect(formService.executeEvent).toHaveBeenCalledWith('testForm', 'svy_default_navigator', 'setSelectedIndex', [5]);
  });

  it('setIndex should default a non-numeric value to 1', () => {
    component.ngOnInit();
    component.setIndex('abc');
    expect(component.navigatorComponentCache.model.currentIndex).toBe(1);
    expect(component.sliderValue).toBe(-1);
  });
});
