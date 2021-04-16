import { browser, by, element } from 'protractor';

export class AppPage {
  navigateTo() {
    console.log(" test go to solution/test_webcomponents_ng2/index.html")
    return browser.get('http://localhost:8183/solution/test_webcomponents_ng2/index.html');
  }

  getParagraphText() {
    const byId= by.id('aa61693e522742cfd7e35df7fe8fb590');
    console.log(byId);
    const elem = element(byId);
    console.log(elem);

    const span = elem.element(by.tagName("div")).all(by.tagName("span")).get(1);
    console.log(span);
    console.log(span.getText());
    return span.getText();
//    const txt = element().getText();
//    debugger;
//    console.log(txt);
//    return element(by.tagName('title')).getText();
  }
}
