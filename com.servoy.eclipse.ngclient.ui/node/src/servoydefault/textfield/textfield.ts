import { Component, OnInit ,Input,Output,EventEmitter} from '@angular/core';

@Component({
  selector: 'servoydefault-textfield',
  templateUrl: './textfield.html',
  styleUrls: ['./textfield.css']
})
export class ServoyDefaultTextField implements OnInit {
  @Input() name;
  @Input() dataprovider
  @Output() dataproviderChange = new EventEmitter();
  
  constructor() { }

  ngOnInit() {
      
  }
  update(val : string) {
      this.dataprovider = val;
      this.dataproviderChange.emit(this.dataprovider);
  }
  
  myapicall(a1,a2,a3) {
      console.log("api call" + a1 + "," + a2 + "," + a3);
  }

}
