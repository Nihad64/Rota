import Foundation
public struct PreferenceCheckpoint:Codable,Sendable {
 public var uid:String?
 public var enabled=false
 public var baseline=SyncEnvelope()
 public init(){}
}
public actor PreferenceStore {
 private let url:URL
 private var state:PreferenceCheckpoint
 public init(directory:URL)throws{
  url=directory.appendingPathComponent("preference-sync.json")
  if FileManager.default.fileExists(atPath:url.path){state=try JSONDecoder().decode(PreferenceCheckpoint.self,from:Data(contentsOf:url));if !state.baseline.records.isEmpty{_ = try PortableSettings.decode(state.baseline.records)}}else{state=PreferenceCheckpoint()}
 }
 public func snapshot()->PreferenceCheckpoint{state}
 private func write(_ next:PreferenceCheckpoint)throws{
  let data=try JSONEncoder().encode(next)
  #if os(iOS)
  try data.write(to:url,options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
  #else
  try data.write(to:url,options:.atomic)
  #endif
  state=next
 }
 public func enable(_ uid:String)throws{guard state.uid==nil||state.uid==uid else{throw RotamDataError.invalid("Tercihler başka bir hesaba bağlı.")};var next=state;next.uid=uid;next.enabled=true;try write(next)}
 public func pause()throws{var next=state;next.enabled=false;try write(next)}
 public func checkpoint(_ baseline:SyncEnvelope,uid:String)throws{guard state.enabled,state.uid==uid else{throw RotamDataError.invalid("Tercih eşitleme izni değişti.")};_ = try PortableSettings.decode(baseline.records);var next=state;next.baseline=baseline;try write(next)}
}
