import XCTest

@MainActor final class RotamUITests:XCTestCase {
 func testOfflineWelcomeAndManualPlanSurviveRelaunch(){
  let app=XCUIApplication();app.launchArguments=["-ui-testing"];app.launch()
  if app.buttons["Hesapsız devam et"].waitForExistence(timeout:15){app.buttons["Hesapsız devam et"].tap();app.buttons["Şimdi değil"].tap()}
  XCTAssertTrue(app.tabBars.buttons["Bugün"].waitForExistence(timeout:10))
  app.buttons["Plan ekle"].firstMatch.tap();app.buttons["Kendin oluştur"].tap()
  let title=app.textFields["plan-title"]
  if title.waitForExistence(timeout:5){title.tap();title.typeText("iOS test plani")}
  else{let multiline=app.textViews["plan-title"];XCTAssertTrue(multiline.waitForExistence(timeout:5));multiline.tap();multiline.typeText("iOS test plani")}
  app.buttons["save-plan"].tap()
  XCTAssertTrue(app.navigationBars.buttons["Kapat"].waitForExistence(timeout:10));app.navigationBars.buttons["Kapat"].tap()
  XCTAssertTrue(app.staticTexts["iOS test plani"].firstMatch.waitForExistence(timeout:10))
  let screenshot=XCTAttachment(screenshot:app.screenshot());screenshot.name="native-today";screenshot.lifetime = .keepAlways;add(screenshot)
  app.terminate();app.launch();XCTAssertTrue(app.staticTexts["iOS test plani"].firstMatch.waitForExistence(timeout:15))
  app.tabBars.buttons["Ayarlar"].tap();XCTAssertTrue(app.staticTexts["Hesabım"].waitForExistence(timeout:5))
  let settings=XCTAttachment(screenshot:app.screenshot());settings.name="native-settings";settings.lifetime = .keepAlways;add(settings)
 }
}
