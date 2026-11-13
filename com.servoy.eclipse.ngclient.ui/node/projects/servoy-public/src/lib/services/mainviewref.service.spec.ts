import { describe, it, expect } from 'vitest';
import { ViewContainerRef } from '@angular/core';
import { MainViewRefService } from './mainviewref.service';

describe('MainViewRefService', () => {
    it('should store and return the main container ref', () => {
        const service = new MainViewRefService();
        const ref = { element: {} } as unknown as ViewContainerRef;
        service.mainContainer = ref;
        expect(service.mainContainer).toBe(ref);
    });

    it('should return undefined before any ref is set', () => {
        const service = new MainViewRefService();
        expect(service.mainContainer).toBeUndefined();
    });
});
