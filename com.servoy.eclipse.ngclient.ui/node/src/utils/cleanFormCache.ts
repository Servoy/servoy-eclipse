import { Injectable } from '@angular/core';
import { FormCache, FormComponentCache, StructureCache } from '../ngclient/types';

@Injectable({
    providedIn: 'root'
})
export class CleanFormCache {
	clean(formCache: FormCache) {
		let deleteFCC = [];
        formCache?.formComponents.forEach(fcc => {
			if (fcc.name.includes('containedForm')) {
				deleteFCC.push(fcc.name);
			}
		});
		
		if (deleteFCC.length > 0) {
			deleteFCC.forEach(fccName => {
				formCache.removeFormComponent(fccName);
			});
			
			let fccItems = [];
			formCache?.formComponents.forEach(fcc => {
				if (fcc.items.length > 0) {
					fccItems.push({fcc, arr: fcc.items.filter(item => item instanceof FormComponentCache)});
				}
			});
			
			this.checkItems(fccItems);
		}
	}
	
	private checkItems(array: Array<{fcc: FormComponentCache, arr: Array<FormComponentCache>}>) {
		for (let i = 0; i < array.length; i++) {
			const {fcc, arr} = array[i];
			if (arr.length > 0) {
				arr.sort((a: FormComponentCache, b: FormComponentCache) => a.name.split('containedForm').length - b.name.split('containedForm').length);
				const check = arr[0].name.split('containedForm').length;
				const deleteFCC = arr.filter((item: FormComponentCache) => item.name.split('containedForm').length !== check);
				const okFCC = arr.filter((item: FormComponentCache) => item.name.split('containedForm').length === check);
				deleteFCC.forEach((item: FormComponentCache) => fcc.removeChild(item));
				const checkComp = okFCC.map((item: FormComponentCache) => item.name);
				let deleteComp = [];
				fcc.items.forEach(item => {
					if (!(item instanceof FormComponentCache) && !(item instanceof StructureCache)) {
						checkComp.forEach((itm: string) => {
							if (item.name.includes(itm)) {
								deleteComp.push(item);
							}
						});
					}
				});
				deleteComp.forEach(item => fcc.removeChild(item));
				let fccItems = [];
				fcc.items.forEach(item => {
					if (item instanceof FormComponentCache) {
						fccItems.push({fcc: item, arr: item.items.filter(item => item instanceof FormComponentCache)});
					}
				});
				this.checkItems(fccItems);
			}	
		}
	}
}