import { describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { ToolbarItemComponent } from './toolbaritem.component';
import { ToolbarItem } from '../toolbar.component';

describe('ToolbarItemComponent.isDisabled', () => {
  let component: ToolbarItemComponent;

  const makeItem = (enabled: (() => boolean) | boolean): ToolbarItem => {
    const item = { enabled: signal<(() => boolean) | boolean>(false) } as ToolbarItem;
    item.enabled.set(enabled);
    return item;
  };

  const setItem = (item: ToolbarItem) => {
    (component as unknown as { item: () => ToolbarItem }).item = () => item;
  };

  beforeEach(() => {
    component = Object.create(ToolbarItemComponent.prototype);
  });

  it('should be disabled when enabled signal holds boolean false', () => {
    setItem(makeItem(false));
    expect(component.isDisabled()).toBe(true);
  });

  it('should be enabled when enabled signal holds boolean true', () => {
    setItem(makeItem(true));
    expect(component.isDisabled()).toBe(false);
  });

  it('should be enabled when enabled signal holds a function returning true', () => {
    setItem(makeItem(() => true));
    expect(component.isDisabled()).toBe(false);
  });

  it('should be disabled when enabled signal holds a function returning false', () => {
    setItem(makeItem(() => false));
    expect(component.isDisabled()).toBe(true);
  });

  it('should re-evaluate a function-valued enabled on each read', () => {
    let allowed = false;
    setItem(makeItem(() => allowed));
    expect(component.isDisabled()).toBe(true);
    allowed = true;
    expect(component.isDisabled()).toBe(false);
  });

  it('should reflect a programmatic enabled.set from a boolean signal', () => {
    const item = makeItem(false);
    setItem(item);
    expect(component.isDisabled()).toBe(true);
    item.enabled.set(true);
    expect(component.isDisabled()).toBe(false);
  });
});
