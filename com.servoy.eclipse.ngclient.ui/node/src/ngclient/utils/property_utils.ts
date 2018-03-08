import { Renderer2} from '@angular/core';
export class PropertyUtils {
    public static setHorizontalAlignment(element:any, renderer:Renderer2, halign) {
        if (halign != -1)
        {
            if (halign == 0)
            {
                renderer.setStyle(element, 'text-align', 'center');
            }
            else if (halign == 4)
            {
                renderer.setStyle(element, 'text-align', 'right');
            }
            else
            {
                renderer.setStyle(element, 'text-align', 'left');
            }
        }
    }
    
   public static  setBorder(element:any, renderer:Renderer2,newVal) {
        if(typeof newVal !== 'object' || newVal == null) {renderer.removeStyle(element,'border'); return;}

        if (renderer.parentNode(element).nodeName == "FIELDSET"){ 
            //unwrap fieldset
            var parent = renderer.parentNode(element);
            renderer.insertBefore(renderer.parentNode(parent),element,parent);
            renderer.removeChild(renderer.parentNode(parent),parent);
        }
        if(newVal.type == "TitledBorder"){
            var fieldset = renderer.createElement("fieldset");
            renderer.setAttribute(fieldset, "style", "padding:1px;margin:0px;border:1px solid silver;width:100%;height:100%");
            
            var legend = renderer.createElement("legend");
            renderer.setAttribute(legend, "style", "border-bottom:0px; margin:0px;width:auto;color:"+newVal.color+";text-align:"+newVal.titleJustification);
            if (newVal.font)
            {
                for (var key in newVal.font) {
                    //keys like 'fontSize' need to be converted into 'font-size'
                    renderer.setStyle(legend, key.replace( /([a-z])([A-Z])/g, '$1-$2' ).toLowerCase(), newVal.font[key]);
                };
            }
            renderer.appendChild(legend, renderer.createText(newVal.title));
            
              // this is the way it is done in the old ngclient, but the actual component is positioned partially outside the border          
//            var parent = renderer.parentNode(element);
//            renderer.insertBefore(parent, fieldset, element);
//            renderer.appendChild(fieldset, legend);
//            renderer.appendChild(fieldset, element);
            
            renderer.appendChild(fieldset, legend);
            for (var i in element.childNodes) {
                if (element.childNodes[i].nodeType == 1) {
                    renderer.appendChild(fieldset, element.childNodes[i]);
                }
            }
            renderer.appendChild(element, fieldset);
            
        }else if(newVal.borderStyle){ 
            renderer.removeStyle(element,'border');
            for (var key in newVal.borderStyle) {
                renderer.setStyle(element, key.replace( /([a-z])([A-Z])/g, '$1-$2' ).toLowerCase(), newVal.borderStyle[key]);
            };
        }
    }
   
   public static addSelectOnEnter(element:any, renderer:Renderer2) {
       renderer.listen(element, "focus", ()=> {
           setTimeout(()=> {
               // this access "document" directly which shoudn't really be done, but angular doesn't have encapsuled support for testing "is(":focus")"
               var currentFocusedElement =  document.querySelector(":focus");
               if (currentFocusedElement == element)
                   element.select(); 
           },0);           
       });
   }
}