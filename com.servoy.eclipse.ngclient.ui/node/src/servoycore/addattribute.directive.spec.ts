import { describe, it, expect, beforeEach } from 'vitest';
import { Component, forwardRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddAttributeDirective } from './addattribute.directive';
import { AbstractFormComponent } from '../ngclient/form/abstract_form_component.component';

@Component({
  selector: 'servoycore-addattr-host',
  template: '<div [svyContainerStyle]="style" [svyContainerLayout]="layout" [svyContainerClasses]="classes" [svyContainerAttributes]="attributes" #target></div>',
  standalone: true,
  imports: [AddAttributeDirective],
  providers: [{ provide: AbstractFormComponent, useExisting: forwardRef(() => AddAttrHostComponent) }],
})
class AddAttrHostComponent extends AbstractFormComponent {
  style: any = undefined;
  layout: Record<string, string> | undefined = undefined;
  classes: string[] = undefined!;
  attributes: any = undefined;

  cssstylesByName: Record<string, Record<string, string>> = {};
  containersByName: Record<string, string[]> = {};

  getFormCache(): any {
    return null;
  }
  getTemplate(): any {
    return null;
  }
  getTemplateForLFC(): any {
    return null;
  }
  getContainerByName(): Element {
    return null!;
  }
}

describe('AddAttributeDirective', () => {
  let fixture: ComponentFixture<AddAttrHostComponent>;
  let host: AddAttrHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddAttrHostComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(AddAttrHostComponent);
    host = fixture.componentInstance;
  });

  it('should resolve the parent AbstractFormComponent via DI', () => {
    const el = fixture.debugElement.children[0];
    const dir = el.injector.get(AddAttributeDirective);
    expect(dir.parent).toBe(host);
  });

  it('should add the container classes to the element', () => {
    host.classes = ['cls-a', 'cls-b'];
    fixture.detectChanges();
    const target = fixture.nativeElement.querySelector('div');
    expect(target.classList.contains('cls-a')).toBe(true);
    expect(target.classList.contains('cls-b')).toBe(true);
  });

  it('should apply the container layout styles to the element', () => {
    host.layout = { color: 'rgb(255, 0, 0)', width: '10px' };
    fixture.detectChanges();
    const target = fixture.nativeElement.querySelector('div');
    expect(target.style.color).toBe('rgb(255, 0, 0)');
    expect(target.style.width).toBe('10px');
  });

  it('should set the container attributes on the element', () => {
    host.attributes = { 'data-x': 'y' };
    fixture.detectChanges();
    const target = fixture.nativeElement.querySelector('div');
    expect(target.getAttribute('data-x')).toBe('y');
  });

  it('should apply relative position and a pixel height for a cssPositionContainer structure', () => {
    host.style = { cssPositionContainer: true };
    host.attributes = { size: { height: 150 } };
    fixture.detectChanges();
    const target = fixture.nativeElement.querySelector('div');
    expect(target.style.position).toBe('relative');
    expect(target.style.height).toBe('150px');
  });
});
