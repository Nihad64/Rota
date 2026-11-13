#if !SWIFT_PACKAGE && DEBUG
import XCTest
@testable import Rotam

@MainActor final class GatewaySDKTests:XCTestCase {
 func testTwoActualAppleSDKClientsRoundTripAndOwnerIsolation()async throws {
  let result=try await FirebaseGateway().exerciseEmulatorContract()
  XCTAssertEqual(2,result.revision)
  XCTAssertEqual(1,result.plans)
  XCTAssertEqual(8,result.preferences)
  XCTAssertTrue(result.rejectedOtherOwner)
 }
}
#endif
