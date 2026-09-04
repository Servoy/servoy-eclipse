import { vi, describe, beforeEach, it, expect } from 'vitest';
import { DynamicGuidesComponent } from './dynamicguides.component';
import { Guide, SnapData } from '../services/dynamicguides.service';

describe('DynamicGuidesComponent', () => {
  let component: DynamicGuidesComponent;
  let clearGuidesSpy: ReturnType<typeof vi.fn>;
  let renderGuidesSpy: ReturnType<typeof vi.fn>;

  const setGuides = (data: SnapData | null) => (component as any).setGuides(data);
  const signalReturning = (value: boolean) => () => value;

  const makeSnapData = (): SnapData => {
    const guide = new Guide(10, 20, 1, 100, 'snap');
    return new SnapData({} as MouseEvent, 20, 10, {}, [guide]);
  };

  beforeEach(() => {
    component = Object.create(DynamicGuidesComponent.prototype);
    clearGuidesSpy = vi.fn();
    renderGuidesSpy = vi.fn();
    (component as any).clearGuides = clearGuidesSpy;
    (component as any).renderGuides = renderGuidesSpy;
    (component as any).editorSession = {
      dragging: signalReturning(true),
      resizing: signalReturning(false)
    };
    component.snapData = null;
  });

  it('should set snapData and render guides when a SnapData is provided while dragging', () => {
    const data = makeSnapData();
    setGuides(data);
    expect(component.snapData).toBe(data);
    expect(clearGuidesSpy).toHaveBeenCalled();
    expect(renderGuidesSpy).toHaveBeenCalled();
  });

  it('should clear snapData and not render when null is provided (SVY-21412)', () => {
    component.snapData = makeSnapData();
    setGuides(null);
    expect(component.snapData).toBeNull();
    expect(clearGuidesSpy).toHaveBeenCalled();
    expect(renderGuidesSpy).not.toHaveBeenCalled();
  });

  it('should reset snapData to null when a SnapData is followed by null (guide clears)', () => {
    const data = makeSnapData();
    setGuides(data);
    expect(component.snapData).toBe(data);

    setGuides(null);
    expect(component.snapData).toBeNull();
    expect(clearGuidesSpy).toHaveBeenCalledTimes(2);
  });

  it('should not throw when null is provided and nothing is being dragged', () => {
    (component as any).editorSession = {
      dragging: signalReturning(false),
      resizing: signalReturning(false)
    };
    expect(() => setGuides(null)).not.toThrow();
    expect(component.snapData).toBeNull();
  });
});
