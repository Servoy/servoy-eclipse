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
       // TODO impl
//        if(typeof newVal !== 'object' || newVal == null) {element.css('border',''); return;}
//
//        if (element.parent().is("fieldset")){ 
//            $(element.parent()).replaceWith($(element));//unwrap fieldset
//        }
//        if(newVal.type == "TitledBorder"){
//            element.wrap('<fieldset style="padding:5px;margin:0px;border:1px solid silver;width:100%;height:100%"></fieldset>')
//            var x = element.parent().prepend("<legend align='"+newVal.titleJustiffication+"' style='border-bottom:0px; margin:0px;width:auto;color:"+
//                    newVal.color+"'>"+newVal.title+"</legend>")
//            if (newVal.font) x.children("legend").css(newVal.font);
//        }else if(newVal.borderStyle){ 
//            element.css('border','')
//            element.css(newVal.borderStyle)
//        }
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