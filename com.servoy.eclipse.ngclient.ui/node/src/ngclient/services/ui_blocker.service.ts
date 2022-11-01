import { ServoyService } from '../servoy.service';

export class UIBlockerService {
	executingEvents = [];

    constructor(private servoyService: ServoyService) {
	}

	public shouldBlockDuplicateEvents(componentId: string, model: any, eventType: string, rowId?: any): boolean {
		let blockDuplicates = null;
		if(model && model.clientProperty && model.clientProperty.ngBlockDuplicateEvents !== undefined) {
			blockDuplicates = model.clientProperty.ngBlockDuplicateEvents;
		} else {
			blockDuplicates = this.servoyService.getUIProperties().getUIProperty('ngBlockDuplicateEvents');
		}
		if (blockDuplicates && componentId && eventType) {
			for (const executeEvent of this.executingEvents) {
				if (executeEvent.componentId === componentId && executeEvent.eventType === eventType && executeEvent.rowId === rowId) {
					return true;
				}
			}
		}
		this.executingEvents.push({componentId, eventType, rowId});
		return false;
	}

	public eventExecuted(componentId: string, model: any, eventType: string, rowId?: any) {
		for (let i = 0; i < this.executingEvents.length; i++) {
			if (this.executingEvents[i].componentId === componentId && this.executingEvents[i].eventType === eventType && this.executingEvents[i].rowId === rowId) {
				this.executingEvents.splice(i,1);
				break;
			}
		}
	}
}
