import { Component, SimpleChanges, Input, Renderer2, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { ServoyBaseComponent } from '../../ngclient/servoy_public';
import { IFoundset } from '../../sablo/spectypes.service';
import { ChartType, ChartOptions, ChartDataSets } from 'chart.js';
import { BaseChartDirective, Label } from 'ng2-charts';

@Component({
    selector: 'svychartjs-chart',
    templateUrl: './chart.html'
})
export class SvyChartJS extends ServoyBaseComponent<HTMLDivElement> {

    @Input() styleClass: string;
    @Input() backgroundColor: string;
    @Input() borderColor: string;
    @Input() borderWidth: string;
    @Input() hoverBackgroundColor: string;
    @Input() hoverBorderColor: string;
    @Input() hoverBorderWidth: string;
    @Input() backgroundColorScheme: string;
    @Input() legendLabel: string;
    @Input() type: ChartType;
    @Input() data: any;
    @Input() options: ChartOptions;
    @Input() plugin: any;
    @Input() collapseOnClick: boolean;
    @Input() foundset: IFoundset;

    @Input() onChartDrawn: () => void;
    @Input() onClick: (datasetIndex: number, index: number, label: string, value: number, event: Event) => void;

    @ViewChild(BaseChartDirective, { static: true }) chart: BaseChartDirective;
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLDivElement>;

    public dataset: ChartDataSets[] = [{}];
    public labels: Label[];

    private removeListenerFunction: () => void;

    constructor(renderer: Renderer2, cdRef: ChangeDetectorRef) {
        super(renderer, cdRef);
    }

    svyOnInit() {
        super.svyOnInit();
        if (!this.options) {
            this.options = {
                responsive: true
            };
        }
        if (this.foundset) {
            this.removeListenerFunction = this.foundset.addChangeListener(() => {
                this.setupData();
            });

        }
    }

    ngOnDestroy(): void {
        if (this.removeListenerFunction != null) {
            this.removeListenerFunction();
            this.removeListenerFunction = null;
        }
    }

    svyOnChanges(changes: SimpleChanges) {
        if (changes) {
            for (const property of Object.keys(changes)) {
                const change = changes[property];
                switch (property) {
                    case 'styleClass':
                        if (change.previousValue)
                            this.renderer.removeClass(this.getNativeElement(), change.previousValue);
                        if (change.currentValue)
                            this.renderer.addClass(this.getNativeElement(), change.currentValue);
                        break;
                    case 'borderColor':
                        this.dataset[0].borderColor = change.currentValue;
                        break;
                    case 'borderWidth':
                        this.dataset[0].borderWidth = change.currentValue;
                        break;
                    case 'hoverBackgroundColor':
                        this.dataset[0].hoverBackgroundColor = change.currentValue;
                        break;
                    case 'hoverBorderColor':
                        this.dataset[0].hoverBorderColor = change.currentValue;
                        break;
                    case 'hoverBorderWidth':
                        this.dataset[0].hoverBorderWidth = change.currentValue;
                        break;
                    case 'legendLabel':
                        this.dataset[0].label = change.currentValue;
                        break;
                    case 'backgroundColorScheme':
                    case 'backgroundColor':
                        if (this.backgroundColor) {
                            this.dataset[0].backgroundColor = this.backgroundColor;
                        } else {
                            let color_scheme = ['#5DA5DA',
                                '#FAA43A',
                                '#60BD68',
                                '#F17CB0',
                                '#B2912F',
                                '#B276B2',
                                '#DECF3F',
                                '#F15854',
                                '#4D4D4D'];

                            if (this.backgroundColorScheme) {
                                color_scheme = this.getColorScheme(this.backgroundColorScheme);
                            }
                            this.dataset[0].backgroundColor = color_scheme;
                        }
                        break;
                    case 'data':
                        this.setupData();
                        if (this.onChartDrawn) {
                            this.onChartDrawn();
                        }
                        break;

                }
            }
        }
        super.svyOnChanges(changes);
    }

    getColorScheme(type: string): Array<string> {
        switch (type) {
            case 'default_color_scheme':
                return ['#5DA5DA',
                    '#FAA43A',
                    '#60BD68',
                    '#F17CB0',
                    '#B2912F',
                    '#B276B2',
                    '#DECF3F',
                    '#F15854',
                    '#4D4D4D'];
            case 'facebook':
                return ['#3b5998',
                    '#8b9dc3',
                    '#dfe3ee',
                    '#f7f7f7',
                    '#ffffff',
                ];
            case 'bootstrap':
                return ['#d9534f',
                    '#f9f9f9',
                    '#5bc0de',
                    '#5cb85c',
                    '#428bca',
                ];
            case 'space_gray':
                return ['#343d46',
                    '#4f5b66',
                    '#65737e',
                    '#a7adba',
                    'c0c5ce',
                ];
            case 'cappuccino':
                return ['#4b3832',
                    '#854442',
                    '#fff4e6',
                    '#3c2f2f',
                    '#be9b7b'];
            case 'beach':
                return ['#96ceb4',
                    '#ffeead',
                    '#ff6f69',
                    '#ffcc5c',
                    '#88d8b0'];
            case 'blues':
                return ['#011f4b',
                    '#03396c',
                    '#005b96',
                    '#6497b1',
                    '#b3cde0'];
            case 'metro':
                return ['#d11141',
                    '#00b159',
                    '#00aedb',
                    '#f37735',
                    '#ffc425'];
            case 'turquoise_shades':
                return ['#b3ecec',
                    '#89ecda',
                    '#43e8d8',
                    '#40e0d0',
                    '#3bd6c6'];
            case 'retro':
                return ['#666547',
                    '#fb2e01',
                    '#6fcb9f',
                    '#ffe28a',
                    '#fffeb3'];
            case 'pastel_rainbow':
                return ['#a8e6cf',
                    '#dcedc1',
                    '#ffd3b6',
                    '#ffaaa5',
                    '#ff8b94'];
            case 'pwc_corp':
                return ['#dc6900',
                    '#eb8c00',
                    '#e0301e',
                    '#a32020',
                    '#602320'];
            case 'sage_cream':
                return ['#bbcbdb',
                    '#9ebd9e',
                    '#dd855c',
                    '#f1e8ca',
                    '#745151'];
            case 'pink_shades':
                return ['#ff00a9',
                    '#fb9f9f',
                    '#ff0065',
                    '#ffbfd3',
                    '#fb5858'];
            case 'craftsman':
                return ['#d7c797',
                    '#845422',
                    '#ead61c',
                    '#a47c48',
                    '#000000'];
            case 'minimal_fire':
                return ['#eec82b',
                    '#d6961c',
                    '#a96232',
                    '#9a2511',
                    '#560000'];
            case 'modern_1':
                return ['#99b898',
                    '#feceab',
                    '#ff847c',
                    '#e84a5f',
                    '#2a363b'];
            case 'modern_2':
                return ['#192425',
                    '#d2aa6b',
                    '#fd9418',
                    '#475758',
                    '#2b6c8c'];
            case 'modern_3':
                return ['#d75c37',
                    '#67727a',
                    '#6991ac',
                    '#c3d7df',
                    '#f5f5f5'];
            case 'modern_muted':
                return ['#a9b7c0',
                    '#c7d8c6',
                    '#efd9c1',
                    '#cccbc6',
                    '#aaaaaa'];
            default:
                break;
        }

    }

    setupData() {
        if (this.foundset) {
            const labels = [];
            this.dataset[0].data = [];
            for (const row of  this.foundset.viewPort.rows) {
                labels.push(row.label ? row.label : row.value);
                this.dataset[0].data.push(row.value);
            }
            //update datamodel
            this.data = {
                type: this.type,
                data: { labels, datasets: [this.dataset] }
            };
        } else if (this.data) {
            this.dataset = this.data.data.datasets;
            this.labels = this.data.data.labels;
        }
    }

    handleClick(e: Event) {
        const activePoints = this.chart.chart.getElementsAtEvent(e);
        const dataset = this.chart.chart.getDatasetAtEvent(e);
        if (!dataset[0]) return;
        //get selected dataset index (helps distinguish between multiple datasets)
        const firstdataset: any= dataset[0];
        const datasetIndex = firstdataset._datasetIndex;
        const selected: any = activePoints[datasetIndex];
        if (!selected) return;
        const label = this.chart.chart.data.labels[selected._index];
        const value = this.chart.chart.data.datasets[selected._datasetIndex].data[selected._index];
        if (this.onClick) {
            this.onClick(datasetIndex, selected._index, label.toString(), value as number, e);
        }
    }

    generateLegend(): string {
        return this.chart.chart.generateLegend().toString();
    }

    getChartAsImage(): string {
        return this.chart.chart.toBase64Image();
    }

    refreshChart() {
        if (!this.data || !this.options) {
            return;
        }
        // update the chart if it already exists
        if (this.chart) {
            this.chart.chart.update();
        }
    }

    clearChart() {
        if (this.chart) {
            this.chart.chart.clear();
        }
    }

    drawChart() {
        // what to do here ?
        this.chart.chart.render();
        if (this.onChartDrawn) {
            this.onChartDrawn();
        }
    }

}

