import { Component, OnInit ,Input,Output,EventEmitter} from '@angular/core';

@Component({
  selector: 'servoydefault-textfield',
  templateUrl: './textfield.html',
  styleUrls: ['./textfield.css']
})
export class ServoyDefaultTextField implements OnInit {
  @Input() name;
  @Input() dataProviderID
  @Output() dataProviderIDChange = new EventEmitter();
  
  constructor() { }

  ngOnInit() {
      
  }
  update(val : string) {
      this.dataProviderID = val;
      this.dataProviderIDChange.emit(this.dataProviderID);
  }
  
  myapicall(a1,a2,a3) {
      console.log("api call" + a1 + "," + a2 + "," + a3);
  }

}
