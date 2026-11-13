import { describe, it, expect } from 'vitest';
import { FormCache, PartCache, ComponentCache, StructureCache, FormComponentCache, FormComponentProperties, Dimension, instanceOfApiExecutor, instanceOfFormComponent } from './types';

const typesRegistryMock = (): any => ({
  getComponentSpecification: () => undefined,
});

describe('FormCache.getBodyPartLayout (SVY-21655)', () => {
  const bodyLayout = { position: 'absolute', 'overflow-x': 'hidden', 'overflow-y': 'hidden' };
  const otherLayout = { position: 'absolute', overflow: 'hidden' };

  const createFormCache = (): FormCache => new FormCache('testform', { width: 640, height: 490 }, false, '', {} as any);

  it('should return the layout of the body part when it is the only part', () => {
    const formCache = createFormCache();
    formCache.addPart(new PartCache('body', ['svy-body'], bodyLayout));

    expect(formCache.getBodyPartLayout()).toEqual(bodyLayout);
  });

  it('should return the layout of the body part when header parts come before it', () => {
    const formCache = createFormCache();
    formCache.addPart(new PartCache('titleheader', ['svy-titleheader'], otherLayout));
    formCache.addPart(new PartCache('header', ['svy-header'], otherLayout));
    formCache.addPart(new PartCache('body', ['svy-body'], bodyLayout));
    formCache.addPart(new PartCache('footer', ['svy-footer'], otherLayout));

    expect(formCache.getBodyPartLayout()).toEqual(bodyLayout);
  });
});

describe('ComponentCache', () => {
  it('should default model to visible true', () => {
    const cc = new ComponentCache('btn', 'mypackage-mybutton', undefined!, [], {}, typesRegistryMock());
    expect(cc.model.visible).toBe(true);
    expect(cc.name).toBe('btn');
    expect(cc.specName).toBe('mypackage-mybutton');
  });

  it('should convert the webobject spec name to a JS name (type) using the dash separator', () => {
    const cc = new ComponentCache('svc', 'testpackage-myTestService', undefined!, [], {}, typesRegistryMock());
    expect(cc.type).toBe('testpackageMyTestService');
  });

  it('should keep a dashless spec name unchanged as type', () => {
    const cc = new ComponentCache('x', 'nodash', undefined!, [], {}, typesRegistryMock());
    expect(cc.type).toBe('nodash');
  });

  it('should prefer the explicit elType over specName for the JS type', () => {
    const cc = new ComponentCache('x', 'specname', 'servoycore-navigator', [], {}, typesRegistryMock());
    expect(cc.type).toBe('servoycoreNavigator');
  });

  it('initForDesigner should copy all initial model properties', () => {
    const cc = new ComponentCache('x', 'y', undefined!, [], {}, typesRegistryMock());
    const result = cc.initForDesigner({ text: 'hello', enabled: false });
    expect(result).toBe(cc);
    expect(cc.model.text).toBe('hello');
    expect(cc.model.enabled).toBe(false);
  });

  it('initForDesignerIsVariant should skip styleClass only when it is a variant', () => {
    const variant = new ComponentCache('x', 'y', undefined!, [], {}, typesRegistryMock());
    variant.initForDesignerIsVariant({ styleClass: 'foo', text: 'bar' }, true);
    expect(variant.model.styleClass).toBeUndefined();
    expect(variant.model.text).toBe('bar');

    const nonVariant = new ComponentCache('x', 'y', undefined!, [], {}, typesRegistryMock());
    nonVariant.initForDesignerIsVariant({ styleClass: 'foo', text: 'bar' }, false);
    expect(nonVariant.model.styleClass).toBeUndefined();
    expect(nonVariant.model.text).toBeUndefined();
  });

  it('sendChanges should be a no-op', () => {
    const cc = new ComponentCache('x', 'y', undefined!, [], {}, typesRegistryMock());
    expect(() => cc.sendChanges('a', 1, 0)).not.toThrow();
  });

  it('toString should include the name and type', () => {
    const cc = new ComponentCache('mybtn', 'pack-mybtn', undefined!, [], {}, typesRegistryMock());
    expect(cc.toString()).toBe('ComponentCache(mybtn, packMybtn)');
  });

  it('should assign unique incremental rId values', () => {
    const a = new ComponentCache('a', 'a', undefined!, [], {}, typesRegistryMock());
    const b = new ComponentCache('b', 'b', undefined!, [], {}, typesRegistryMock());
    expect(b.rId).toBeGreaterThan(a.rId);
  });
});

describe('StructureCache', () => {
  it('name should prefer the id over the tagname', () => {
    expect(new StructureCache('div', [], {}, [], 'cont1').name).toBe('cont1');
    expect(new StructureCache('div', [], {}, [], undefined).name).toBe('div');
  });

  it('addChild should append and set the parent for a nested structure', () => {
    const parent = new StructureCache('div', []);
    const child = new StructureCache('span', []);
    const returned = parent.addChild(child);
    expect(returned).toBe(child);
    expect(parent.items[0]).toBe(child);
    expect(child.parent).toBe(parent);
  });

  it('addChild should set the parent for a component but return null', () => {
    const parent = new StructureCache('div', []);
    const comp = new ComponentCache('c', 'c', undefined!, [], {}, typesRegistryMock());
    const returned = parent.addChild(comp);
    expect(returned).toBeNull();
    expect(comp.parent).toBe(parent);
  });

  it('addChild with insertBefore should splice at the right index', () => {
    const parent = new StructureCache('div', []);
    const first = new StructureCache('a', []);
    const second = new StructureCache('b', []);
    parent.addChild(first);
    parent.addChild(second, first);
    expect(parent.items).toEqual([second, first]);
  });

  it('removeChild should remove an existing child and return true', () => {
    const parent = new StructureCache('div', []);
    const child = new StructureCache('span', []);
    parent.addChild(child);
    expect(parent.removeChild(child)).toBe(true);
    expect(parent.items.length).toBe(0);
  });

  it('removeChild should return false for an unknown child', () => {
    const parent = new StructureCache('div', []);
    const other = new StructureCache('span', []);
    expect(parent.removeChild(other)).toBe(false);
  });

  it('getDepth should count the parent chain', () => {
    const root = new StructureCache('root', []);
    const mid = new StructureCache('mid', []);
    const leaf = new StructureCache('leaf', []);
    root.addChild(mid);
    mid.addChild(leaf);
    expect(root.getDepth()).toBe(-1);
    expect(mid.getDepth()).toBe(0);
    expect(leaf.getDepth()).toBe(1);
  });

  it('toString should include the id', () => {
    expect(new StructureCache('div', [], {}, [], 'id9').toString()).toBe('StructureCache(id9)');
  });
});

describe('PartCache', () => {
  it('addChild should skip the default navigator component', () => {
    const part = new PartCache('body', [], {});
    const nav = new ComponentCache('n', 'specname', 'servoycore-navigator', [], {}, typesRegistryMock());
    part.addChild(nav);
    expect(part.items.length).toBe(0);
  });

  it('addChild should add a normal component', () => {
    const part = new PartCache('body', [], {});
    const comp = new ComponentCache('c', 'c', undefined!, [], {}, typesRegistryMock());
    part.addChild(comp);
    expect(part.items[0]).toBe(comp);
  });
});

describe('FormComponentCache', () => {
  const props = new FormComponentProperties([], {}, {});
  const create = () => new FormComponentCache('fc', 'fc', undefined!, [], false, {}, props, false, typesRegistryMock());

  it('should extend ComponentCache', () => {
    expect(create()).toBeInstanceOf(ComponentCache);
  });

  it('addChild should add a structure child', () => {
    const fc = create();
    const child = new StructureCache('div', []);
    fc.addChild(child);
    expect(fc.items[0]).toBe(child);
  });

  it('addChild should skip the navigator component', () => {
    const fc = create();
    const nav = new ComponentCache('n', 's', 'servoycore-navigator', [], {}, typesRegistryMock());
    fc.addChild(nav);
    expect(fc.items.length).toBe(0);
  });

  it('removeChild should remove existing child', () => {
    const fc = create();
    const child = new StructureCache('div', []);
    fc.addChild(child);
    expect(fc.removeChild(child)).toBe(true);
    expect(fc.items.length).toBe(0);
  });

  it('removeChild should return false for unknown child', () => {
    const fc = create();
    expect(fc.removeChild(new StructureCache('div', []))).toBe(false);
  });

  it('initForDesigner should return the FormComponentCache and set the model', () => {
    const fc = create();
    const result = fc.initForDesigner({ a: 1 });
    expect(result).toBe(fc);
    expect(fc.model.a).toBe(1);
  });
});

describe('FormCache component/formComponent management', () => {
  const create = () => new FormCache('testform', { width: 10, height: 10 }, false, '', typesRegistryMock());
  const comp = (name: string) => new ComponentCache(name, name, undefined!, [], {}, typesRegistryMock());
  const formComp = (name: string) => new FormComponentCache(name, name, undefined!, [], false, {}, new FormComponentProperties([], {}, {}), false, typesRegistryMock());

  it('absolute should be the opposite of responsive', () => {
    expect(create().absolute).toBe(true);
    expect(new FormCache('f', { width: 1, height: 1 }, true, '', typesRegistryMock()).absolute).toBe(false);
  });

  it('add should register a component into componentCache', () => {
    const fc = create();
    fc.add(comp('btn'));
    expect(fc.getComponent('btn')).toBeDefined();
  });

  it('add should register a FormComponentCache in formComponents', () => {
    const fc = create();
    const c = formComp('fc1');
    fc.add(c);
    expect(fc.getFormComponent('fc1')).toBe(c);
    expect(fc.getComponent('fc1')).toBe(c);
  });

  it('add with a PartCache parent should push into partComponentsCache', () => {
    const fc = create();
    const part = new PartCache('body', [], {});
    const c = comp('x');
    fc.add(c, part);
    expect(fc.partComponentsCache).toContain(c);
  });

  it('addLayoutContainer should index by id', () => {
    const fc = create();
    const s = new StructureCache('div', [], {}, [], 'lc1');
    fc.addLayoutContainer(s);
    expect(fc.getLayoutContainer('lc1')).toBe(s);
  });

  it('addLayoutContainer should ignore containers without an id', () => {
    const fc = create();
    const s = new StructureCache('div', []);
    fc.addLayoutContainer(s);
    expect(fc.layoutContainersCache.size).toBe(0);
  });

  it('getPart should find a part by name', () => {
    const fc = create();
    const part = new PartCache('header', [], {});
    fc.addPart(part);
    expect(fc.getPart('header')).toBe(part);
    expect(fc.getPart('nope')).toBeUndefined();
  });

  it('removeComponent should remove the component from the cache', () => {
    const fc = create();
    const c = comp('x');
    fc.add(c);
    fc.removeComponent('x');
    expect(fc.getComponent('x')).toBeUndefined();
  });

  it('removeLayoutContainer should remove the container', () => {
    const fc = create();
    const s = new StructureCache('div', [], {}, [], 'lc1');
    fc.addLayoutContainer(s);
    fc.removeLayoutContainer('lc1');
    expect(fc.getLayoutContainer('lc1')).toBeUndefined();
  });

  it('removeFormComponent should remove the form component', () => {
    const fc = create();
    const c = formComp('fc1');
    fc.add(c);
    fc.removeFormComponent('fc1');
    expect(fc.getFormComponent('fc1')).toBeUndefined();
  });

  it('getLayoutContainerByFCName should find the single container holding the named form component', () => {
    const fc = create();
    const inner = formComp('innerFC');
    const container = new StructureCache('div', [], {}, [inner], 'lc1');
    fc.addLayoutContainer(container);
    expect(fc.getLayoutContainerByFCName('innerFC')).toBe(container);
    expect(fc.getLayoutContainerByFCName('unknown')).toBeUndefined();
  });

  it('mainStructure setter should walk and register nested components', () => {
    const fc = create();
    const inner = comp('inner');
    const nestedStructure = new StructureCache('div', [], {}, [inner], 'nested');
    const main = new StructureCache('div', [], {}, [nestedStructure]);
    fc.mainStructure = main;
    expect(fc.mainStructure).toBe(main);
    expect(fc.getComponent('inner')).toBe(inner);
    expect(fc.getLayoutContainer('nested')).toBe(nestedStructure);
  });

  it('getComponentSpecification should return undefined for unknown components', () => {
    const fc = create();
    expect(fc.getComponentSpecification('unknown')).toBeUndefined();
  });

  it('getClientSideType should fall back to the dynamic client side types', () => {
    const fc = create();
    const c = comp('btn');
    (c.dynamicClientSideTypes as any)['someProp'] = { fromServerToClient: () => null } as any;
    fc.add(c);
    expect(fc.getClientSideType('btn', 'someProp')).toBeDefined();
    expect(fc.getClientSideType('btn', 'missing')).toBeUndefined();
  });
});

describe('type guards', () => {
  it('instanceOfApiExecutor should detect a callApi function', () => {
    expect(instanceOfApiExecutor({ callApi: () => null })).toBe(true);
    expect(instanceOfApiExecutor({})).toBe(false);
    expect(instanceOfApiExecutor(null)).toBe(false);
  });

  it('instanceOfFormComponent should detect a detectChanges function', () => {
    expect(instanceOfFormComponent({ detectChanges: () => undefined })).toBe(true);
    expect(instanceOfFormComponent({})).toBe(false);
    expect(instanceOfFormComponent(undefined)).toBe(false);
  });
});

describe('Dimension', () => {
  it('should default to zero width and height', () => {
    const d = new Dimension();
    expect(d.width).toBe(0);
    expect(d.height).toBe(0);
  });
});
