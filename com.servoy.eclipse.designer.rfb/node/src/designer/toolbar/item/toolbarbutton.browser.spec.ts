import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { signal, provideZonelessChangeDetection } from '@angular/core';
import { ToolbarButtonComponent } from './toolbarbutton.component';
import { ToolbarItem } from '../toolbar.component';

describe('ToolbarButtonComponent (browser)', () => {
  let fixture: ComponentFixture<ToolbarButtonComponent>;

  const makeItem = (enabled: (() => boolean) | boolean): ToolbarItem => {
    const item = {
      text: 'Zoom in',
      icon: 'zoomin.png',
      list: undefined,
      state: signal<boolean | undefined>(undefined),
      enabled: signal<(() => boolean) | boolean>(false),
      onclick: null
    } as unknown as ToolbarItem;
    item.enabled.set(enabled);
    return item;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolbarButtonComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();

    fixture = TestBed.createComponent(ToolbarButtonComponent);
  });

  afterEach(() => {
    fixture?.destroy();
  });

  const button = (): HTMLButtonElement => fixture.nativeElement.querySelector('button');

  it('should render the button disabled when enabled signal starts false', async () => {
    fixture.componentRef.setInput('item', makeItem(false));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(button().disabled).toBe(true);
  });

  it('should clear the disabled attribute when enabled.set(true) is called from outside an Angular event', async () => {
    const item = makeItem(false);
    fixture.componentRef.setInput('item', item);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(button().disabled).toBe(true);

    item.enabled.set(true);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(button().disabled).toBe(false);
  });

  it('should set the disabled attribute when enabled.set(false) is called from outside an Angular event', async () => {
    const item = makeItem(true);
    fixture.componentRef.setInput('item', item);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(button().disabled).toBe(false);

    item.enabled.set(false);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(button().disabled).toBe(true);
  });

  it('should derive disabled from a function-valued enabled', async () => {
    fixture.componentRef.setInput('item', makeItem(() => false));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(button().disabled).toBe(true);

    fixture.componentRef.setInput('item', makeItem(() => true));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(button().disabled).toBe(false);
  });
});
