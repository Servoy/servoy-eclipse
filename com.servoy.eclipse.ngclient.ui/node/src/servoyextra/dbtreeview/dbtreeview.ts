import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostListener, Input, Renderer2, SimpleChanges, ViewChild } from "@angular/core";
import { ServoyBaseComponent } from "../../ngclient/basecomponent";
import { IActionMapping, ITreeOptions, TreeComponent, TreeNode} from '@circlon/angular-tree-component'
import { LoggerService, LoggerFactory } from "../../sablo/logger.service";
import { ApplicationService } from "../../ngclient/services/application.service";
import { ServoyService } from "../../ngclient/servoy.service";

@Component({
    selector: 'servoyextra-dbtreeview', 
    templateUrl: './dbtreeview.html', 
    styleUrls: ['./dbtreeview.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyExtraDbtreeview extends ServoyBaseComponent<HTMLDivElement> { 

    @Input() levelVisibilityChange;
    @Input() foundsets: any[]; 
    @Input() relatedFoundsets: any[];
    @Input() roots;
    @Input() autoRefresh;
    @Input() bindings;
    @Input() cssPosition;
    @Input() enabled;
    @Input() levelVisibility;
    @Input() location;
    @Input() responsiveHeight;
    @Input() selection;
    @Input() size;
    @Input() nodes: any[];
    @Input() visible: boolean;
    @Input() onReady: (e: Event, data?: any) => void;

    log: LoggerService;
    folderImgPath = "../../assets/images/folder.png"
    fileImgPath = "../../assets/images/file.png"
    useCheckboxes = false;
    expandedNodes: any;
    displayNodes = [];

    @ViewChild('element', { static: true }) elementRef: ElementRef;
    @ViewChild('tree', { static: true }) tree: TreeComponent;
  
    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef,logFactory: LoggerFactory,
       private applicationService: ApplicationService,
       private servoyService: ServoyService) {
      super(renderer, cdRef);
      this.log = logFactory.getLogger('ServoyExtraDbtreeview');
    }

    @HostListener('window:beforeunload', ['$event'])
    async onBeforeUnloadHander(event) {
      console.log(event);
      await this.servoyApi.callServerSideApi('saveNodes', [this.displayNodes]);
    }

    actionMapping: IActionMapping = {
      mouse: {
        click: (tree, node) => { 
          this.check(node, !node.data.checked);
          if(node.data && node.data.callbackinfo) {
            const doubleClick = node.data.callbackinfo;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.callbackinfoParamValue]);
          }
        },
        dblClick: (tree, node) => {
          if(node.data && node.data.methodToCallOnDoubleClick) {
            const doubleClick = node.data.methodToCallOnDoubleClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.methodToCallOnDoubleClickParamValue]);
          }
        }, 
        contextMenu: (tree, node) => {
          if(node.data && node.data.methodToCallOnRightClick) {
            const doubleClick = node.data.methodToCallOnRightClick;
            this.servoyService.executeInlineScript(doubleClick.formname, doubleClick.script, [doubleClick.methodToCallOnRightClickParamValue]);
          }
        }
      }
    };

    options: ITreeOptions = {
      getChildren: this.getChildren.bind(this),
      actionMapping: this.actionMapping
    }

    svyOnInit() {
        super.svyOnInit();
        if (!this.nodes || this.nodes.length === 0) { this.initTree() }  else {
          this.displayNodes = this.nodes;
        }
    }

    svyOnChanges(changes: SimpleChanges) {
      if (changes) {
        for (const property of Object.keys(changes)) {
            const change = changes[property];
            switch (property) {
                case 'responsiveHeight':
                    if(this.elementRef && this.servoyApi.isInAbsoluteLayout() && change.currentValue ) {
                      this.renderer.setStyle(this.elementRef, 'height', change.currentValue + 'px');
                    }
                    break;
                case 'foundsets': {
                  this.refresh();
                  break;
                }
                case 'selection': {
                  this.selectNode(change.currentValue);
                  break;
                }
                case 'levelVisibility': {
                  if (change.currentValue && this.tree) {
                    this.expandChildNodes(this.tree.treeModel.virtualRoot, change.currentValue.level, change.currentValue.state);
                    break;
                  }
                }
                case 'enabled': {
                  if(change.previousValue !== change.currentValue && this.elementRef) {
                    if(change.currentValue) {
                      this.renderer.removeClass(this.elementRef, "dbtreeview-disabled");
                    } else {
                      this.renderer.addClass(this.elementRef, "dbtreeview-disabled");
                    }
                  }
                  break;
                }
              }
            }
          }
    }

    async getChildren(node: TreeNode) {
      console.log(node);
      let children = [];
      for (let index = 0; index < this.relatedFoundsets.length; index++) {
        let relatedFoundsetsChecked = false;
        const rows = this.relatedFoundsets[index].foundset.viewPort.rows;
        for(const row of rows) {
          if (row.parent_id === node.data.id) {
            if (!relatedFoundsetsChecked!) {
              await this.servoyApi.callServerSideApi('loadRelatedFoundset', [index]);
              relatedFoundsetsChecked = true;
            }
            children.push(this.buildChild(row, this.relatedFoundsets[index], index));
          }
        }
      }
      return children;
    }

    onNodeExpanded(event: any) {
      event.node.data["expanded"] = event.isExpanded;
    }

    onTreeLoad(event) {
      this.expandNodes(this.nodes);
      if (this.onReady) {
        this.onReady(event);
      }
    }

    expandNodes(nodes) {
      nodes.forEach(node => {
        if (node.expanded) {
          this.tree.treeModel.getNodeById(node.id).setIsExpanded(true);
        }
        if (node.hasChildren) this.expandNodes(node.children);
      });
    }

    check(node, checked) {
      this.updateChildNodeCheckbox(node, checked);
      this.updateParentNodeCheckbox(node.realParent);
    }

    updateChildNodeCheckbox(node, checked) {
      node.data.checked = checked;
      if (node.children) {
        node.children.forEach((child) => this.updateChildNodeCheckbox(child, checked));
      }
    }

    updateParentNodeCheckbox(node) {
      if (!node) {
        return;
      }
      let allChildrenChecked = true;
      let noChildChecked = true;
  
      for (const child of node.children) {
        if (!child.data.checked || child.data.indeterminate) {
          allChildrenChecked = false;
        }
        if (child.data.checked) {
          noChildChecked = false;
        }
      }
  
      if (allChildrenChecked) {
        node.data.checked = true;
        node.data.indeterminate = false;
      } else if (noChildChecked) {
        node.data.checked = false;
        node.data.indeterminate = false;
      } else {
        node.data.checked = true;
        node.data.indeterminate = true;
      }
      this.updateParentNodeCheckbox(node.parent);
    }

    private initTree(): void {
        this.foundsets.forEach((elem) => {
            let children = [];
            elem.foundset.viewPort.rows.forEach((row, index) => {
              let child = this.buildChild(row, elem, index);
              children.push(child);
            });
            this.displayNodes = children;
            this.nodes.push(children); 
            this.cdRef.detectChanges();
        }, this);
    }

    private buildChild(row: any, foundsetInfo: any, index: any) {
      let child = { id: row.node_id, name: row.text, hasChildren: this.hasChildren(row.node_id) };

      // TODO get the foundsetpk
      const foundsetpk = 'node_id';
      const binding = this.getBinding(foundsetInfo.datasourceID);
      if (binding.imageurldataprovider) {
        child["image"] = this.getIconURL(foundsetInfo.foundset.viewPort.rows[index][binding.imageurldataprovider]);
      } else if (child.hasChildren) {
        child["image"] = this.folderImgPath;
      } else {
        child["image"] = this.fileImgPath;
      }

      if (binding.checkboxvaluedataprovider) {
        child["checkbox"] = Boolean(foundsetInfo.foundset.viewPort.rows[index][binding.hascheckboxdataprovider]);
      }
      else if (binding.hasCheckboxValue) {
        child["checkbox"] = binding.hasCheckboxValue.indexOf("" + foundsetInfo.foundset.viewPort.rows[index][foundsetpk]) != -1;
      }
      else {
        child["checkbox"] = Boolean(binding.initialCheckboxValues);
      }
      if (child["checkbox"]) {
        // if(parentItem && parentItem.selected) {
        //   child.selected = true;
        // }
        if (binding.checkboxvaluedataprovider) {
          child["checked"] = Boolean(foundsetInfo.foundset.viewPort.rows[index][binding.checkboxvaluedataprovider]);
        }
        else if (binding.initialCheckboxValues) {
          child["checked"] = binding.initialCheckboxValues.indexOf("" + foundsetInfo.foundset.viewPort.rows[index][foundsetpk]) != -1;
        }
      }

      // TODO get level
      let level = 1;
      var isLevelVisible = this.levelVisibility && this.levelVisibility.state && (this.levelVisibility.level == level);

      if(binding.callbackinfo || binding.methodToCallOnCheckBoxChange || binding.methodToCallOnDoubleClick || binding.methodToCallOnRightClick)
          {
            if(binding.callbackinfo) {
              child["callbackinfo"] = binding.callbackinfo.f;
              child["callbackinfoParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.callbackinfo.param];
            }
            if(binding.methodToCallOnCheckBoxChange) {
              child["methodToCallOnCheckBoxChange"] = binding.methodToCallOnCheckBoxChange.f;
              child["methodToCallOnCheckBoxChangeParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnCheckBoxChange.param];
            }
            if(binding.methodToCallOnDoubleClick) {
              child["methodToCallOnDoubleClick"] = binding.methodToCallOnDoubleClick.f;
              child["methodToCallOnDoubleClickParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnDoubleClick.param];
            }    				    				
            if(binding.methodToCallOnRightClick) {
              child["methodToCallOnRightClick"] = binding.methodToCallOnRightClick.f;
              child["methodToCallOnRightClickParamValue"] = foundsetInfo.foundset.viewPort.rows[index][binding.methodToCallOnRightClick.param];
            }    				    				
          }

      return child;
    }

    hasChildren(nodeID: number) {
      let hasChildren = false;
      this.relatedFoundsets.forEach((foundsetInfo) => {
          if (foundsetInfo.foundset.viewPort.rows.find(row => row.parent_id === nodeID)) { 
            hasChildren = true;
            return;
          }
          if(hasChildren) return;
      }, this);
      return hasChildren;
    }

    // TODO remove it when you're sure you don't need it anymore. 
    // isFoundsetAlreadyLoaded(id: number) {
    //   for (let index = 0; index < this.relatedFoundsets.length; index++) {
    //     const rows = this.relatedFoundsets[index].viewPort.rows;
    //     for(const row of rows) {
    //       if (row.parent_id === id) {
    //         return true;
    //       }
    //     }
    //   }
    //   return false;
    // }


    public refresh(): void {}

    public isNodeExpanded(pk: Array<number>): boolean {
      if (this.tree) {
          const node = this.tree.treeModel.getNodeByPath(pk);
          if(node) {
	  				return node.isExpanded;
	  			}
      }
      return false;
    }

    public setExpandNode(pk: Array<number>, state: boolean): void {
      if (this.tree) {
        if (pk && pk.length) {
          if(state) {
            this.expandedNodes = pk.slice(0, pk.length);
          }
          const node = this.tree.treeModel.getNodeByPath(pk);
          if(node) {
            this.tree.treeModel.setExpandedNode(node, state);
          }
        }
      }
    }

    public getSelectionPath() {
      return this.selection;
    }

    public updateCheckBoxValues(datasource, pk, state): boolean {
      return false;
    }

    public getCheckBoxValues(datasource) {}

    expandChildNodes(node: TreeNode, level: number, state: boolean) {
      if(level >= 1) {
        let nodeChildren = node.children;
        nodeChildren.forEach(child => {
          if (state) {
            child.setActiveAndVisible(true);
            child.setIsExpanded(state);
          } else if (level === 1) {
            child.setIsExpanded(state);
          }
        });
      }
    }

    isNodeSelected(node, selection) {
			if(selection && selection.length) {
				var nodePKPath = [];
				nodePKPath.unshift(this.getPKFromNodeKey(node.key));
				var parentNode = node.data.parentItem;
				while(parentNode) {
					nodePKPath.unshift(this.getPKFromNodeKey(parentNode.key));
					parentNode = parentNode.data.parentItem;
				}

				if(nodePKPath.length == selection.length) {
					for(var i = 0; i < nodePKPath.length; i++) {
						if(nodePKPath[i] != selection[i].toString()) {
							return false;
						}
					}
					return true;
				}
			}

			return false;
		}		

    selectNode(selection: Array<number>) {
			if(selection && selection.length) {
				this.expandedNodes = selection.slice(0, selection.length);
        const node = this.tree.treeModel.getNodeByPath(selection);
				if(node && !node.isActive) {
					node.setActiveAndVisible(true);
				}
				else {
					this.refresh();
				}
	    }
  	}

    getPKFromNodeKey(nodeKey) {
			var pkIdx = nodeKey.indexOf('_');
			return nodeKey.substring(pkIdx + 1);
		}

    private getBinding(datasource) {
      for(let i = 0; i < this.bindings.length; i++) {
        if(datasource == this.bindings[i].datasource) {
          return this.bindings[i];
        }
      }
      return null;
    }

    getIconURL(iconPath: string) {
      if(iconPath && iconPath.indexOf("media://") == 0) {
        return "resources/fs/" + this.applicationService.getSolutionName() + iconPath.substring(8);
      }
      return iconPath;
    }
 
    private getDataproviders(datasource, foundsetpk) {
      let dataproviders = {}
      const binding = this.getBinding(datasource);
      
      dataproviders[foundsetpk] = foundsetpk;
      
      if(binding.textdataprovider) {
        dataproviders[binding.textdataprovider] = binding.textdataprovider;
      }
      if(binding.textdataprovider) {
        dataproviders[binding.textdataprovider] = binding.textdataprovider;
      }
      if(binding.hascheckboxdataprovider) {
        dataproviders[binding.hascheckboxdataprovider] = binding.hascheckboxdataprovider;
      }
      if(binding.checkboxvaluedataprovider) {
        dataproviders[binding.checkboxvaluedataprovider] = binding.checkboxvaluedataprovider;
      }
      if(binding.tooltiptextdataprovider) {
        dataproviders[binding.tooltiptextdataprovider] = binding.tooltiptextdataprovider;
      }
      if(binding.imageurldataprovider) {
        dataproviders[binding.imageurldataprovider] = binding.imageurldataprovider;
      }
      if(binding.childsortdataprovider) {
        dataproviders[binding.childsortdataprovider] = binding.childsortdataprovider;
      }    		    		
      if(binding.callbackinfo) {
        dataproviders[binding.callbackinfo.param] = binding.callbackinfo.param;
      }
      if(binding.methodToCallOnCheckBoxChange) {
        dataproviders[binding.methodToCallOnCheckBoxChange.param] = binding.methodToCallOnCheckBoxChange.param;
      }    		
      if(binding.methodToCallOnDoubleClick) {
        dataproviders[binding.methodToCallOnDoubleClick.param] = binding.methodToCallOnDoubleClick.param;
      }    		    		
      if(binding.methodToCallOnRightClick) {
        dataproviders[binding.methodToCallOnRightClick.param] = binding.methodToCallOnRightClick.param;
      }    		    		

      return dataproviders;
    }
}