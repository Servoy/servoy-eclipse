import { AppPage } from './app.po';

describe('ngclient2 App', () => {
  let page: AppPage;

  beforeEach(() => {
    page = new AppPage();
  });

  it('should display the username label', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Username');
  });
});
